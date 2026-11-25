export type DataMode = 'mock' | 'api';

/**
 * Defaults to 'api' (Spring Boot REST backend) in integrated runtime.
 * Set NEXT_PUBLIC_DATA_MODE=mock to use in-memory frontend fixtures offline.
 */
export const DATA_MODE: DataMode =
  process.env.NEXT_PUBLIC_DATA_MODE === 'mock' ? 'mock' : 'api';

export const IS_MOCK_MODE = DATA_MODE === 'mock';
