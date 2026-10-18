package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 调用方须先持有会员积分账户行锁，所有发分和兑换共用这一锁序。 */
@Service
public class PointsRecoveryService {
    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;
    public PointsRecoveryService(JdbcTemplate jdbc,CustomerRepository customers){this.jdbc=jdbc;this.customers=customers;}
    public long outstanding(String customerId){return jdbc.queryForObject("SELECT COALESCE(sum(remaining_points),0) FROM customer_points_recoveries WHERE customer_id=? AND status='pending'",Long.class,customerId);}
    public void requireClear(String customerId){if(outstanding(customerId)>0) throw ApiException.conflict("POINTS_RECOVERY_PENDING","尚有待追回积分，暂不能兑换");}
    public long consume(String customerId,long awarded,long balanceAfter,String source,Instant now){
        long left=awarded;
        record Recovery(String id,long remaining){}
        List<Recovery> rows=jdbc.query("SELECT reward_id,remaining_points FROM customer_points_recoveries WHERE customer_id=? AND status='pending' ORDER BY created_at,reward_id FOR UPDATE",(rs,n)->new Recovery(rs.getString(1),rs.getLong(2)),customerId);
        for(Recovery row:rows){
            long paid=Math.min(left,row.remaining());if(paid==0)break;left-=paid;
            jdbc.update("UPDATE customer_points_recoveries SET remaining_points=?,status=?,updated_at=? WHERE reward_id=?",row.remaining()-paid,row.remaining()==paid?"recovered":"pending",Timestamp.from(now),row.id());
            customers.savePointsLedger(new CustomerPointsLedgerRecord("ledger_recover_"+UUID.randomUUID().toString().replace("-",""),customerId,-paid,balanceAfter,"deduct",row.id(),"后续积分抵扣待追回："+source,null,null,false,now,"deduct","confirmed","ticket_points_recovery",UUID.randomUUID().toString()));
        }
        return awarded-left;
    }
    public void cancelForDeregistration(String customerId,Instant now){jdbc.update("UPDATE customer_points_recoveries SET status='cancelled',remaining_points=0,updated_at=? WHERE customer_id=? AND status='pending'",Timestamp.from(now),customerId);}
}
