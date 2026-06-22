import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ApiResponse, Page} from '../../core/models';

interface Part {
  id: string;
  partNo: string;
  partName: string;
  price: number;
  quantity: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface PartRequest {
  partNo: string;
  partName: string;
  price: number;
  quantity?: number;
}

/** Payload điều chỉnh tồn kho: delta dương = nhập thêm, âm = xuất bớt. */
export interface AdjustPartStockRequest {
  reason: string;
  delta: number;
}

@Injectable({
  providedIn: 'root',
})
export class PartsService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/parts';

  search(query: {
    page: number;
    size: number;
    partNo?: string;
    partName?: string;
  }): Observable<ApiResponse<Page<Part>>> {
    const params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('partNo', query.partNo ?? '')
      .set('partName', query.partName ?? '');

    return this.http.get<ApiResponse<Page<Part>>>(this.apiUrl, {params});
  }

  create(payload: PartRequest): Observable<Part> {
    return this.http.post<Part>(this.apiUrl, payload);
  }


  update(id: string, payload: PartRequest): Observable<Part> {
    return this.http.put<Part>(`${this.apiUrl}/${id}`, payload);
  }

  adjustPartStock(id: string, payload: AdjustPartStockRequest): Observable<Part> {
    return this.http.post<Part>(`${this.apiUrl}/${id}/stock-adjustments`, payload);
  }
}
