import { useState } from 'react';
import type { Page } from '../api/types';
import { useLoad } from './useLoad';

export function usePaged<T>(load: (page: number) => Promise<Page<T>>) {
  const [page, setPage] = useState(0);
  return { ...useLoad(() => load(page), [page]), setPage };
}
