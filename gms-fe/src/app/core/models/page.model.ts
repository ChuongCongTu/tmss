/** Kết quả phân trang trả về từ Spring Data (Page<T>). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // page index hiện tại (0-based)
  size: number;
}
