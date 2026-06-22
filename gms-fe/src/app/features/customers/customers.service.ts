import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ApiResponse, Page} from '../../core/models';

interface Customer {
  id: string;
  fullName: string;
  address: string;
  phone: string;
  createdAt: Date;
  updatedAt: Date;
}

/** Payload tạo mới — chỉ các field người dùng nhập, không có id/timestamps (hệ thống tự sinh). */
export interface CustomerRequest {
  fullName: string;
  phone: string;
  address: string;
}

@Injectable({
  providedIn: 'root',
})
export class CustomersService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/customers';

  /** Lấy danh sách khách hàng có phân trang + lọc theo fullName/phone. */
  search(query: {
    page: number;
    size: number;
    fullName?: string;
    phone?: string;
  }): Observable<ApiResponse<Page<Customer>>> {
    const params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('fullName', query.fullName ?? '')
      .set('phone', query.phone ?? '');

    return this.http.get<ApiResponse<Page<Customer>>>(this.apiUrl, {params});
  }

  create(payload: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  update(id: string, payload: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/${id}`, payload);
  }
}
