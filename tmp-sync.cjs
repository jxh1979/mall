
const YAML=require("yaml");const fs=require("fs");
const y=YAML.parse(fs.readFileSync("docs/openapi/openapi-p0.yaml","utf8"));
const j=JSON.parse(fs.readFileSync("docs/openapi/openapi-p0.json","utf8"));
const pth="/api/v1/analytics/finance-receivables/debtors";
if(!y.paths[pth])throw new Error("missing");
j.paths[pth]=y.paths[pth];
fs.writeFileSync("docs/openapi/openapi-p0.json",JSON.stringify(j,null,2)+"
");
function canon(o){if(Array.isArray(o))return o.map(canon);if(o&&typeof o==="object"){const r={};for(const k of Object.keys(o).sort())r[k]=canon(o[k]);return r;}return o;}
const y2=YAML.parse(fs.readFileSync("docs/openapi/openapi-p0.yaml","utf8"));
const j2=JSON.parse(fs.readFileSync("docs/openapi/openapi-p0.json","utf8"));
if(JSON.stringify(canon(y2))!==JSON.stringify(canon(j2)))throw new Error("mismatch");
console.log("json synced",Object.keys(j2.paths).length);
