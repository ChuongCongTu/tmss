/** Khung response chuẩn của backend GMS: { status, message, data }. */
export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}