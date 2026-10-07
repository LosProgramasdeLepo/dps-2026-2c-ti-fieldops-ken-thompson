import { ApiError } from '../api/http';
import { Icon } from './Icon';

export function ErrorNotice({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  const message = error instanceof Error ? error.message : String(error);
  const details = error instanceof ApiError ? error.details : [];
  return (
    <div className="error" role="alert">
      <Icon name="alert" />
      <div>
        <strong>{message}</strong>
        {details.length > 0 && (
          <ul>
            {details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
