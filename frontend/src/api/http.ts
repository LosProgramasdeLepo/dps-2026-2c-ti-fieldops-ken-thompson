export class ApiError extends Error {
  readonly status: number;
  readonly details: string[];

  constructor(status: number, message: string, details: string[]) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

interface Problem {
  title?: string;
  detail?: string;
  errors?: string[];
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw await toError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toError(response: Response): Promise<ApiError> {
  const problem: Problem = await response.json().catch(() => ({}));
  return new ApiError(response.status, problem.detail ?? problem.title ?? response.statusText, problem.errors ?? []);
}

export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T = void>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: (path: string, body?: unknown) => request<void>('PUT', path, body),
  delete: (path: string) => request<void>('DELETE', path),
};
