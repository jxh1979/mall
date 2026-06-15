import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));

const requiredRequestBodies = [
  "OpenAuthTokenRequest",
  "Webhook",
  "CustomerTicketOrderRequest",
  "Verification",
  "CustomerBarReservationRequest",
  "BarOrderRequest",
  "PaymentIntent",
  "AmusementSafetyActionRequest",
  "ProviderAdapterTestRequest"
];

const requiredResponses = ["Success", "Page", "WebhookAccepted"];
const errors = [];

function hasExample(container, name) {
  const content = container?.[name]?.content?.["application/json"];
  return Boolean(content?.example || content?.examples);
}

for (const name of requiredRequestBodies) {
  if (!hasExample(openapi.components?.requestBodies, name)) {
    errors.push(`requestBody ${name} is missing an application/json example`);
  }
}

for (const name of requiredResponses) {
  if (!hasExample(openapi.components?.responses, name)) {
    errors.push(`response ${name} is missing an application/json example`);
  }
}

const coverage = openapi["x-p0-example-coverage"];
if (!coverage || !Array.isArray(coverage.request_bodies) || !Array.isArray(coverage.responses)) {
  errors.push("x-p0-example-coverage summary is missing");
}

if (errors.length > 0) {
  console.error("OpenAPI P0 example verification failed:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`OpenAPI P0 example verification passed: ${requiredRequestBodies.length} request bodies and ${requiredResponses.length} responses checked.`);
