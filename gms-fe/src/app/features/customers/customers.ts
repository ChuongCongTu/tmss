import { Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiResponse } from '../../core/models';

interface Customer {
  id: string;
  fullName: string;
  address: string;
  phone: string;
}

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [],
  templateUrl: './customers.html',
  styleUrl: './customers.css',
})
export class Customers implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  customers: Customer[] = [];
  error = '';

  ngOnInit(): void {
    this.http
      .get<ApiResponse<Customer[]>>('http://localhost:8080/api/customers')
      .subscribe({
        next: (res) => (this.customers = res.data ?? []),
        error: () => (this.error = 'Không tải được danh sách khách hàng'),
      });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
