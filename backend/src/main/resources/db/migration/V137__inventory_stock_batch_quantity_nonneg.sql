-- 2e 批次 FIFO/FEFO 扣减：放宽 inventory_stock_batches.quantity CHECK 为 >= 0。
-- 背景：此前入库从不创建批次行、出库从不扣批次 quantity，批次表孤立无数据基础（过期/召回/FIFO 无从追溯）。
-- 本批引入"入库建批次 + 出库 FEFO 逐批扣减"，批次扣减至 0 时 status→consumed 需保留行（quantity=0），
-- 而 V027 的 CHECK (quantity > 0) 会阻止 quantity=0 的 consumed 批次行。改为 >= 0。
-- 用 DO 块动态删除原有 quantity>0 的 CHECK（其名为 PostgreSQL 自动生成，不依赖固定名）。
DO $$
DECLARE c text;
BEGIN
  FOR c IN
    SELECT conname FROM pg_constraint
     WHERE conrelid = 'inventory_stock_batches'::regclass
       AND contype = 'c'
       AND pg_get_constraintdef(oid) LIKE '%quantity%>%0%'
  LOOP
    EXECUTE 'ALTER TABLE inventory_stock_batches DROP CONSTRAINT ' || quote_ident(c);
  END LOOP;
END $$;

ALTER TABLE inventory_stock_batches
    ADD CONSTRAINT chk_inventory_stock_batch_quantity_nonneg CHECK (quantity >= 0);
