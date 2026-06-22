import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {CustomersService} from '../customers/customers.service';
import {catchError, debounceTime, distinctUntilChanged, of, Subject, switchMap, tap} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {PartsService} from './parts.service';
import {AuthService, Role} from '../../core/auth.service';

interface Part {
  id: string;
  partNo: string;
  partName: string;
  price: number;
  quantity: number;
  createdAt: Date;
  updatedAt: Date;
}

@Component({
  selector: 'app-parts',
  imports: [
    ReactiveFormsModule,
    FormsModule
  ],
  templateUrl: './parts.html',
  styleUrl: './parts.css',
})
export class Parts implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly partsService = inject(PartsService);
  private readonly authService = inject(AuthService);

  // state phân trang
  page = signal(0);
  size = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);
  partNo = '';
  partName = '';
  loading = signal(false);
  saving = signal(false);

  parts = signal<Part[]>([]);
  errorMsg = signal('');
  isShowModel = signal(false);
  // null = đang thêm mới; có id = đang sửa khách hàng đó
  editingId = signal<string | null>(null);
  role = signal<Role | null>(this.authService.getRole());

  // điều chỉnh tồn kho
  isShowStockModal = signal(false);
  adjustingPart = signal<Part | null>(null);
  stockErrorMsg = signal('');

  // mỗi khi cần tải lại danh sách thì bắn vào đây
  private readonly reload$ = new Subject<void>();
  // riêng cho ô search để debounce
  private readonly search$ = new Subject<string>();

  form = this.fb.nonNullable.group({
    partNo: ['', [Validators.required, Validators.maxLength(20)]],
    partName: [''],
    price: [0, Validators.required],
    quantity: [0, Validators.required],
  });

  stockForm = this.fb.nonNullable.group({
    delta: [0, [Validators.required]],
    reason: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.reload$
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMsg.set('');
        }),
        switchMap(() =>
          this.fetchParts().pipe(
            catchError(() => {
              this.errorMsg.set('Không tải được danh sách phụ tùng');
              this.loading.set(false);
              return of(null);
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res) => {
        if (!res) return; // trường hợp lỗi đã xử lý ở catchError
        const page = res.data;
        this.parts.set(page?.content ?? []);
        this.totalPages.set(page?.totalPages ?? 0);
        this.totalElements.set(page?.totalElements ?? 0);
        this.loading.set(false);
      });

    // 2. Ô search — debounce, bỏ giá trị trùng, reset về trang 0 rồi tải lại
    this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.page.set(0);
        this.reload$.next();
      });

    // 3. Tải lần đầu
    this.reload$.next();
  }

  private fetchParts() {
    return this.partsService.search({
      page: this.page(),
      size: this.size(),
      partNo: this.partNo,
      partName: this.partName,
    });
  }

  onSearchChange() {
    this.search$.next(`${this.partNo}|${this.partName}`);
  }

  goTo(p: number): void {
    if (p >= 0 && p < this.totalPages()) {
      this.page.set(p);
      this.reload$.next();
    }
  }

  next(): void { this.goTo(this.page() + 1); }
  prev(): void { this.goTo(this.page() - 1); }

  changeSize(s: number): void {
    this.size.set(s);
    this.page.set(0);
    this.reload$.next();
  }

  get f() {
    return this.form.controls;
  }

  /** Mở modal ở chế độ thêm mới. */
  openCreateModal(): void {
    this.editingId.set(null);
    this.form.reset();
    this.isShowModel.set(true);
  }

  /** Mở modal ở chế độ sửa: nạp dữ liệu khách hàng vào form. */
  openEditModal(part: Part): void {
    this.editingId.set(part.id);
    this.form.setValue({
      partNo: part.partNo,
      partName: part.partName,
      price: part.price ?? 0,
      quantity: part.quantity ?? 0,
    });
    this.isShowModel.set(true);
  }

  onCreateOrUpdatePart() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);

    const id = this.editingId();
    const payload = this.form.getRawValue();
    const request$ = id
      ? this.partsService.update(id, payload)
      : this.partsService.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeCreateModal();
        // thêm mới thì về trang đầu; sửa thì giữ nguyên trang đang xem
        if (!id) {
          this.page.set(0);
        }
        this.reload$.next();
      },
      error: () => {
        this.errorMsg.set(id ? 'Cập nhật thất bại. Vui lòng thử lại.' : 'Lưu thất bại. Vui lòng thử lại.');
        this.saving.set(false);
      },
    });
  }

  closeCreateModal(): void {
    this.isShowModel.set(false);
    this.editingId.set(null);
    this.form.reset();
  }

  get sf() {
    return this.stockForm.controls;
  }

  /** Mở form điều chỉnh tồn kho cho 1 phụ tùng. */
  openStockModal(part: Part): void {
    this.adjustingPart.set(part);
    this.stockForm.reset({ delta: 0, reason: '' });
    this.stockErrorMsg.set('');
    this.isShowStockModal.set(true);
  }

  closeStockModal(): void {
    this.isShowStockModal.set(false);
    this.adjustingPart.set(null);
    this.stockErrorMsg.set('');
    this.stockForm.reset();
  }

  onAdjustStock(): void {
    const part = this.adjustingPart();
    if (!part || this.stockForm.invalid) {
      this.stockForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.stockErrorMsg.set('');

    this.partsService
      .adjustPartStock(part.id, this.stockForm.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.closeStockModal();
          this.reload$.next();
        },
        error: (err) => {
          // hiển thị message thật từ backend nếu có, ngay trên popup
          this.stockErrorMsg.set(
            err?.error?.message ?? 'Điều chỉnh tồn kho thất bại. Vui lòng thử lại.'
          );
          this.saving.set(false);
        },
      });
  }

}
