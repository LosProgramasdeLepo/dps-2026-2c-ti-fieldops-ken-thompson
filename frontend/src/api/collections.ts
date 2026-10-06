import { http } from './http';
import type { Page } from './types';

export const PAGE_SIZE = 10;
const MAX_PAGE_SIZE = 100;

export interface Collection<T> {
  page: (page: number, size?: number) => Promise<Page<T>>;
  all: () => Promise<T[]>;
}

export function pageOf<T>(path: string, page: number, size = PAGE_SIZE): Promise<Page<T>> {
  return http.get<Page<T>>(`${path}?page=${page}&size=${size}`);
}

export function collection<T>(path: string): Collection<T> {
  return {
    page: (page, size) => pageOf<T>(path, page, size),
    all: async () => {
      const items: T[] = [];
      for (let page = 0; ; page++) {
        const current = await pageOf<T>(path, page, MAX_PAGE_SIZE);
        items.push(...current.items);
        if (page + 1 >= current.totalPages) {
          return items;
        }
      }
    },
  };
}
