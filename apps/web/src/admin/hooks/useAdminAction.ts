import React from "react";

export interface AdminActionState {
  busyAction: string;
  formError: string;
  setFormError: (message: string) => void;
  runAction: (
    action: string,
    request: Record<string, unknown>,
    call: () => Promise<unknown>,
  ) => Promise<boolean>;
}

/**
 * Centralizes the action-execution boilerplate repeated verbatim across admin
 * pages: track the busy action, clear/surface errors, record the action result
 * via the page's own makeResult, reload on success, and record a failed result
 * on error. The makeResult and optional onSuccess hooks stay per-page so this is
 * invisible to the DOM and to Playwright (action-result payloads are unchanged).
 *
 * Callbacks are read through a ref so inline closures do not destabilize the
 * returned runAction identity.
 */
export function useAdminAction(options: {
  reload: () => Promise<void>;
  setGlobalError: (message: string | null) => void;
  setActionResult: (result: unknown) => void;
  makeResult: (action: string, request: Record<string, unknown>, result: unknown) => unknown;
  fallbackMessage: string;
  onSuccess?: (result: unknown) => void;
}): AdminActionState {
  const { fallbackMessage } = options;
  const [busyAction, setBusyAction] = React.useState("");
  const [formError, setFormError] = React.useState("");

  const optionsRef = React.useRef(options);
  optionsRef.current = options;

  const runAction = React.useCallback(
    async (action: string, request: Record<string, unknown>, call: () => Promise<unknown>) => {
      const current = optionsRef.current;
      setBusyAction(action);
      setFormError("");
      current.setGlobalError(null);
      try {
        const result = await call();
        current.onSuccess?.(result);
        current.setActionResult(current.makeResult(action, request, result));
        await current.reload();
        return true;
      } catch (actionError) {
        const message = actionError instanceof Error ? actionError.message : fallbackMessage;
        setFormError(message);
        current.setGlobalError(message);
        current.setActionResult({ action, status: "failed", message });
        return false;
      } finally {
        setBusyAction("");
      }
    },
    [fallbackMessage],
  );

  return { busyAction, formError, setFormError, runAction };
}
