import { chromium, type FullConfig } from "@playwright/test";
import { installMockApi } from "./mock-api";

// Warms the vite dev server's on-demand module compilation before the parallel
// worker tests start. On a high-core host Playwright spins up many workers, and the
// first test in each spec file otherwise races a cold dev server: the initial
// per-module transforms for the login + admin shell graph exceed the per-test
// timeout. Compiling that shared graph once here keeps the first real tests fast
// and the acceptance run deterministic.
async function globalSetup(config: FullConfig) {
  const project = config.projects[0];
  const baseURL = (project?.use?.baseURL as string | undefined) ?? "http://127.0.0.1:5173";
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage();
    await installMockApi(page);
    await page.goto(`${baseURL}/admin/overview`, { waitUntil: "domcontentloaded", timeout: 90_000 });
    await page.getByTestId("login-view").waitFor({ state: "visible", timeout: 90_000 });
    await page.getByTestId("login-username").fill("admin");
    await page.getByTestId("login-password").fill("admin-pass");
    await page.getByTestId("login-submit").click();
    await page.getByTestId("admin-shell").waitFor({ state: "visible", timeout: 90_000 });
    await page
      .locator(".loading-state")
      .waitFor({ state: "detached", timeout: 90_000 })
      .catch(() => {});
  } catch (error) {
    // Warmup is best-effort; never fail the suite here. A failed warmup just means
    // the first real tests pay the cold-start cost as before.
    console.warn(`[global-setup] dev server warmup did not fully complete: ${(error as Error).message}`);
  } finally {
    await browser.close();
  }
}

export default globalSetup;
