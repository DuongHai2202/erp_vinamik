"""Generate classic UML Class Diagram for ERP Vinamik matching user's visual style."""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS_DIR = ROOT / "docs"

# Canvas dimensions
WIDTH = 1800
HEIGHT = 1350

svg_content = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {WIDTH} {HEIGHT}" width="{WIDTH}" height="{HEIGHT}" style="background-color: #ffffff; font-family: 'Segoe UI', Arial, sans-serif;">
  <defs>
    <!-- Graph paper background grid -->
    <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
      <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#f1f5f9" stroke-width="1"/>
    </pattern>
    <style>
      .pkg-box {{ fill: none; stroke: #64748b; stroke-width: 1.5; stroke-dasharray: 4,4; rx: 6; }}
      .pkg-label {{ font-size: 13px; font-weight: bold; fill: #475569; text-transform: uppercase; letter-spacing: 0.5px; }}

      .uml-box {{ fill: #ffffff; stroke: #1e293b; stroke-width: 1.3; }}
      .uml-header-text {{ font-size: 13.5px; font-weight: bold; fill: #0f172a; text-anchor: middle; }}
      .uml-stereo {{ font-size: 10px; font-style: italic; fill: #64748b; text-anchor: middle; }}
      .uml-attr-text {{ font-size: 11.5px; fill: #1e293b; font-family: 'Segoe UI', Arial, sans-serif; }}
      .uml-method-text {{ font-size: 11.5px; fill: #0f172a; font-family: 'Segoe UI', Arial, sans-serif; }}
      .uml-line {{ stroke: #1e293b; stroke-width: 1.3; fill: none; }}
      .uml-cardinality {{ font-size: 12px; fill: #0f172a; font-weight: 600; font-family: 'Segoe UI', Arial, sans-serif; }}

      .title-text {{ font-size: 20px; font-weight: bold; fill: #0f172a; }}
      .subtitle-text {{ font-size: 13px; fill: #64748b; }}
    </style>
  </defs>

  <!-- Background grid -->
  <rect width="100%" height="100%" fill="url(#grid)" />

  <!-- Diagram Title -->
  <g transform="translate(60, 40)">
    <text x="0" y="0" class="title-text">HỆ THỐNG ERP VINAMIK — BIỂU ĐỒ LỚP CHI TIẾT (UML CLASS DIAGRAM)</text>
    <text x="0" y="22" class="subtitle-text">Kiến trúc nghiệp vụ thực tế: Quản lý Sản xuất &bull; Quản lý Kho vật tư &bull; Quản lý Nhân sự &bull; Nền tảng Định danh</text>
  </g>

  <!-- ==================== PACKAGE 1: SẢN XUẤT ==================== -->
  <rect x="50" y="90" width="560" height="1200" class="pkg-box" />
  <text x="70" y="115" class="pkg-label">&laquo;Package&raquo; Quản lý Sản xuất (Production)</text>

  <!-- ==================== PACKAGE 2: KHO & VẬT TƯ ==================== -->
  <rect x="640" y="90" width="560" height="1200" class="pkg-box" />
  <text x="660" y="115" class="pkg-label">&laquo;Package&raquo; Quản lý Kho &amp; Nguyên vật liệu (Inventory)</text>

  <!-- ==================== PACKAGE 3: NHÂN SỰ & HỆ THỐNG ==================== -->
  <rect x="1230" y="90" width="520" height="1200" class="pkg-box" />
  <text x="1250" y="115" class="pkg-label">&laquo;Package&raquo; Nhân sự &amp; Nền tảng (HR &amp; Identity)</text>


  <!-- ============================================================ -->
  <!--                      CONNECTING LINES                        -->
  <!-- ============================================================ -->

  <!-- KeHoachSanXuat -> LenhSanXuat (Vertical) -->
  <line x1="180" y1="365" x2="180" y2="440" class="uml-line" />
  <text x="165" y="385" class="uml-cardinality">1</text>
  <text x="155" y="425" class="uml-cardinality">0..*</text>

  <!-- LenhSanXuat -> SanLuongThanhPham (Vertical) -->
  <line x1="180" y1="715" x2="180" y2="790" class="uml-line" />
  <text x="165" y="735" class="uml-cardinality">1</text>
  <text x="155" y="775" class="uml-cardinality">0..*</text>

  <!-- LenhSanXuat -> NhuCauVatTu (Horizontal/Diagonal) -->
  <line x1="300" y1="520" x2="360" y2="520" class="uml-line" />
  <text x="310" y="510" class="uml-cardinality">1</text>
  <text x="335" y="510" class="uml-cardinality">1..*</text>

  <!-- LenhSanXuat -> PhanCongSanXuat (Diagonal across to HR) -->
  <line x1="300" y1="620" x2="360" y2="790" class="uml-line" />
  <text x="310" y="640" class="uml-cardinality">1</text>
  <text x="340" y="775" class="uml-cardinality">0..*</text>

  <!-- DinhMucBOM -> LenhSanXuat -->
  <line x1="470" y1="365" x2="280" y2="440" class="uml-line" />
  <text x="445" y="385" class="uml-cardinality">1</text>
  <text x="290" y="425" class="uml-cardinality">0..*</text>

  <!-- DinhMucBOM -> VatTu (BOM refers to raw materials) -->
  <line x1="580" y1="240" x2="670" y2="240" class="uml-line" />
  <text x="590" y="230" class="uml-cardinality">0..*</text>
  <text x="650" y="230" class="uml-cardinality">1..*</text>

  <!-- NhuCauVatTu -> VatTu -->
  <line x1="580" y1="480" x2="670" y2="280" class="uml-line" />
  <text x="590" y="470" class="uml-cardinality">0..*</text>
  <text x="645" y="300" class="uml-cardinality">1</text>

  <!-- VatTu -> SoCaiBienDongKho -->
  <line x1="780" y1="365" x2="780" y2="440" class="uml-line" />
  <text x="765" y="385" class="uml-cardinality">1</text>
  <text x="755" y="425" class="uml-cardinality">0..*</text>

  <!-- SoCaiBienDongKho -> SoDuTonKho -->
  <line x1="780" y1="675" x2="780" y2="750" class="uml-line" />
  <text x="765" y="695" class="uml-cardinality">1</text>
  <text x="765" y="735" class="uml-cardinality">1</text>

  <!-- PhieuNhapKho -> SoCaiBienDongKho -->
  <line x1="950" y1="520" x2="890" y2="520" class="uml-line" />
  <text x="930" y="510" class="uml-cardinality">1</text>
  <text x="900" y="510" class="uml-cardinality">0..*</text>

  <!-- PhieuXuatKho -> SoCaiBienDongKho -->
  <line x1="950" y1="840" x2="890" y2="600" class="uml-line" />
  <text x="930" y="820" class="uml-cardinality">1</text>
  <text x="900" y="625" class="uml-cardinality">0..*</text>

  <!-- SanLuongThanhPham -> PhieuNhapKho (Finished goods receipt request) -->
  <line x1="300" y1="850" x2="950" y2="580" class="uml-line" />
  <text x="315" y="840" class="uml-cardinality">1</text>
  <text x="925" y="605" class="uml-cardinality">0..1</text>

  <!-- NhanVien -> PhanCongSanXuat (HR to Assignment) -->
  <line x1="1360" y1="415" x2="580" y2="850" class="uml-line" />
  <text x="1330" y="435" class="uml-cardinality">1</text>
  <text x="600" y="840" class="uml-cardinality">0..*</text>

  <!-- KeHoachSanXuat -> NhanVien (nguoi_lap_id) -->
  <line x1="300" y1="200" x2="1250" y2="200" class="uml-line" />
  <text x="310" y="190" class="uml-cardinality">0..*</text>
  <text x="1230" y="190" class="uml-cardinality">1</text>

  <!-- PhongBan -> NhanVien -->
  <line x1="1360" y1="715" x2="1360" y2="415" class="uml-line" />
  <text x="1370" y="695" class="uml-cardinality">1</text>
  <text x="1370" y="435" class="uml-cardinality">0..*</text>

  <!-- NhanVien -> TaiKhoanNguoiDung -->
  <line x1="1470" y1="280" x2="1540" y2="280" class="uml-line" />
  <text x="1480" y="270" class="uml-cardinality">1</text>
  <text x="1520" y="270" class="uml-cardinality">0..1</text>

  <!-- TaiKhoanNguoiDung -> VaiTro -->
  <line x1="1650" y1="415" x2="1650" y2="480" class="uml-line" />
  <text x="1660" y="435" class="uml-cardinality">1..*</text>
  <text x="1660" y="465" class="uml-cardinality">1..*</text>


  <!-- ============================================================ -->
  <!--                      CLASSES DEFINITIONS                     -->
  <!-- ============================================================ -->

  <!-- 1. Kế Hoạch Sản Xuất -->
  <g id="class_ke_hoach">
    <rect x="80" y="140" width="220" height="225" class="uml-box" />
    <line x1="80" y1="175" x2="300" y2="175" class="uml-line" />
    <line x1="80" y1="315" x2="300" y2="315" class="uml-line" />

    <text x="190" y="162" class="uml-header-text">Kế Hoạch Sản Xuất</text>

    <text x="90" y="192" class="uml-attr-text">-id: Long</text>
    <text x="90" y="207" class="uml-attr-text">-ma_ke_hoach: String</text>
    <text x="90" y="222" class="uml-attr-text">-ten_ke_hoach: String</text>
    <text x="90" y="237" class="uml-attr-text">-ngay_bat_dau: Date</text>
    <text x="90" y="252" class="uml-attr-text">-ngay_ket_thuc: Date</text>
    <text x="90" y="267" class="uml-attr-text">-trang_thai: String</text>
    <text x="90" y="282" class="uml-attr-text">-nguoi_lap_id: Long</text>
    <text x="90" y="297" class="uml-attr-text">-ghi_chu: String</text>

    <text x="90" y="333" class="uml-method-text">+taoKeHoach(): void</text>
    <text x="90" y="349" class="uml-method-text">+duyetKeHoach(): void</text>
  </g>

  <!-- 2. Lệnh Sản Xuất -->
  <g id="class_lenh_san_xuat">
    <rect x="80" y="440" width="220" height="275" class="uml-box" />
    <line x1="80" y1="475" x2="300" y2="475" class="uml-line" />
    <line x1="80" y1="655" x2="300" y2="655" class="uml-line" />

    <text x="190" y="462" class="uml-header-text">Lệnh Sản Xuất</text>

    <text x="90" y="492" class="uml-attr-text">-id: Long</text>
    <text x="90" y="507" class="uml-attr-text">-ma_lenh: String</text>
    <text x="90" y="522" class="uml-attr-text">-ke_hoach_id: Long</text>
    <text x="90" y="537" class="uml-attr-text">-thanh_pham_id: Long</text>
    <text x="90" y="552" class="uml-attr-text">-bom_id: Long</text>
    <text x="90" y="567" class="uml-attr-text">-so_luong_muc_tieu: Double</text>
    <text x="90" y="582" class="uml-attr-text">-ngay_bat_dau: Date</text>
    <text x="90" y="597" class="uml-attr-text">-ngay_ket_thuc: Date</text>
    <text x="90" y="612" class="uml-attr-text">-day_chuyen: String</text>
    <text x="90" y="627" class="uml-attr-text">-trang_thai: String</text>
    <text x="90" y="642" class="uml-attr-text">-ghi_chu: String</text>

    <text x="90" y="673" class="uml-method-text">+phatHanhLenh(): void</text>
    <text x="90" y="689" class="uml-method-text">+capNhatTienDo(): void</text>
    <text x="90" y="705" class="uml-method-text">+hoanThanhLenh(): void</text>
  </g>

  <!-- 3. Định Mức Nguyên Vật Liệu (BOM) -->
  <g id="class_bom">
    <rect x="360" y="140" width="220" height="225" class="uml-box" />
    <line x1="360" y1="175" x2="580" y2="175" class="uml-line" />
    <line x1="360" y1="315" x2="580" y2="315" class="uml-line" />

    <text x="470" y="162" class="uml-header-text">Định Mức BOM</text>

    <text x="370" y="192" class="uml-attr-text">-id: Long</text>
    <text x="370" y="207" class="uml-attr-text">-ma_bom: String</text>
    <text x="370" y="222" class="uml-attr-text">-thanh_pham_id: Long</text>
    <text x="370" y="237" class="uml-attr-text">-phien_ban: Integer</text>
    <text x="370" y="252" class="uml-attr-text">-so_luong_co_so: Double</text>
    <text x="370" y="267" class="uml-attr-text">-ngay_hieu_luc: Date</text>
    <text x="370" y="282" class="uml-attr-text">-trang_thai: String</text>
    <text x="370" y="297" class="uml-attr-text">-ghi_chu: String</text>

    <text x="370" y="333" class="uml-method-text">+tinhNhuCau(soLuong): List</text>
    <text x="370" y="349" class="uml-method-text">+pheDuyetBOM(): void</text>
  </g>

  <!-- 4. Nhu Cầu Vật Tư Theo Lệnh -->
  <g id="class_nhu_cau_nvl">
    <rect x="360" y="440" width="220" height="235" class="uml-box" />
    <line x1="360" y1="475" x2="580" y2="475" class="uml-line" />
    <line x1="360" y1="620" x2="580" y2="620" class="uml-line" />

    <text x="470" y="462" class="uml-header-text">Nhu Cầu Vật Tư Lệnh</text>

    <text x="370" y="492" class="uml-attr-text">-id: Long</text>
    <text x="370" y="507" class="uml-attr-text">-lenh_san_xuat_id: Long</text>
    <text x="370" y="522" class="uml-attr-text">-vat_tu_id: Long</text>
    <text x="370" y="537" class="uml-attr-text">-so_luong_dinh_muc: Double</text>
    <text x="370" y="552" class="uml-attr-text">-so_luong_yeu_cau: Double</text>
    <text x="370" y="567" class="uml-attr-text">-so_luong_da_cap: Double</text>
    <text x="370" y="582" class="uml-attr-text">-so_luong_thuc_te: Double</text>
    <text x="370" y="597" class="uml-attr-text">-trang_thai_cap: String</text>

    <text x="370" y="638" class="uml-method-text">+guiYeuCauXuatKho(): void</text>
    <text x="370" y="654" class="uml-method-text">+kiemTraThieuHut(): Double</text>
  </g>

  <!-- 5. Phân Công Nhân Sự Sản Xuất -->
  <g id="class_phan_cong">
    <rect x="360" y="790" width="220" height="240" class="uml-box" />
    <line x1="360" y1="825" x2="580" y2="825" class="uml-line" />
    <line x1="360" y1="975" x2="580" y2="975" class="uml-line" />

    <text x="470" y="812" class="uml-header-text">Phân Công Sản Xuất</text>

    <text x="370" y="842" class="uml-attr-text">-id: Long</text>
    <text x="370" y="857" class="uml-attr-text">-lenh_san_xuat_id: Long</text>
    <text x="370" y="872" class="uml-attr-text">-nhan_vien_id: Long</text>
    <text x="370" y="887" class="uml-attr-text">-ca_lam_id: Long</text>
    <text x="370" y="902" class="uml-attr-text">-cong_doan: String</text>
    <text x="370" y="917" class="uml-attr-text">-ngay_lam: Date</text>
    <text x="370" y="932" class="uml-attr-text">-vai_tro: String</text>
    <text x="370" y="947" class="uml-attr-text">-trang_thai: String</text>
    <text x="370" y="962" class="uml-attr-text">-ghi_chu: String</text>

    <text x="370" y="993" class="uml-method-text">+phanCong(): void</text>
    <text x="370" y="1009" class="uml-method-text">+kiemTraTrungLich(): Boolean</text>
  </g>

  <!-- 6. Sản Lượng & Thành Phẩm Hoàn Thành -->
  <g id="class_san_luong">
    <rect x="80" y="790" width="220" height="240" class="uml-box" />
    <line x1="80" y1="825" x2="300" y2="825" class="uml-line" />
    <line x1="80" y1="975" x2="300" y2="975" class="uml-line" />

    <text x="190" y="812" class="uml-header-text">Sản Lượng Thành Phẩm</text>

    <text x="90" y="842" class="uml-attr-text">-id: Long</text>
    <text x="90" y="857" class="uml-attr-text">-lenh_san_xuat_id: Long</text>
    <text x="90" y="872" class="uml-attr-text">-thanh_pham_id: Long</text>
    <text x="90" y="887" class="uml-attr-text">-ma_lo: String</text>
    <text x="90" y="902" class="uml-attr-text">-so_luong_dat: Double</text>
    <text x="90" y="917" class="uml-attr-text">-so_luong_loi: Double</text>
    <text x="90" y="932" class="uml-attr-text">-ngay_san_xuat: Date</text>
    <text x="90" y="947" class="uml-attr-text">-han_su_dung: Date</text>
    <text x="90" y="962" class="uml-attr-text">-trang_thai_nhap_kho: String</text>

    <text x="90" y="993" class="uml-method-text">+ghiNhanSanLuong(): void</text>
    <text x="90" y="1009" class="uml-method-text">+guiYeuCauNhapKho(): void</text>
  </g>


  <!-- ==================== PACKAGE 2 CLASSES (KHO) ==================== -->

  <!-- 7. Vật Tư & Mặt Hàng (StockItem) -->
  <g id="class_vat_tu">
    <rect x="670" y="140" width="220" height="225" class="uml-box" />
    <line x1="670" y1="175" x2="890" y2="175" class="uml-line" />
    <line x1="670" y1="315" x2="890" y2="315" class="uml-line" />

    <text x="780" y="162" class="uml-header-text">Vật Tư &amp; Mặt Hàng</text>

    <text x="680" y="192" class="uml-attr-text">-id: Long</text>
    <text x="680" y="207" class="uml-attr-text">-ma_vat_tu: String</text>
    <text x="680" y="222" class="uml-attr-text">-ten_vat_tu: String</text>
    <text x="680" y="237" class="uml-attr-text">-loai_mat_hang: String</text>
    <text x="680" y="252" class="uml-attr-text">-don_vi_tinh: String</text>
    <text x="680" y="267" class="uml-attr-text">-muc_ton_toi_thieu: Double</text>
    <text x="680" y="282" class="uml-attr-text">-quan_ly_theo_lo: Boolean</text>
    <text x="680" y="297" class="uml-attr-text">-trang_thai: String</text>

    <text x="680" y="333" class="uml-method-text">+kiemTraTonKho(): Double</text>
    <text x="680" y="349" class="uml-method-text">+kichHoat(): void</text>
  </g>

  <!-- 8. Sổ Cái Biến Động Kho (StockMovement - Append-only) -->
  <g id="class_so_cai">
    <rect x="670" y="440" width="220" height="235" class="uml-box" />
    <line x1="670" y1="475" x2="890" y2="475" class="uml-line" />
    <line x1="670" y1="620" x2="890" y2="620" class="uml-line" />

    <text x="780" y="462" class="uml-header-text">Sổ Cái Biến Động Kho</text>

    <text x="680" y="492" class="uml-attr-text">-id: Long</text>
    <text x="680" y="507" class="uml-attr-text">-vat_tu_id: Long</text>
    <text x="680" y="522" class="uml-attr-text">-kho_id: Long</text>
    <text x="680" y="537" class="uml-attr-text">-vi_tri_id: Long</text>
    <text x="680" y="552" class="uml-attr-text">-lo_hang_id: Long?</text>
    <text x="680" y="567" class="uml-attr-text">-so_luong_bien_dong: Double</text>
    <text x="680" y="582" class="uml-attr-text">-loai_giao_dich: String</text>
    <text x="680" y="597" class="uml-attr-text">-chung_tu_nguon_id: Long</text>

    <text x="680" y="638" class="uml-method-text">+ghiSoBatBien(): void</text>
    <text x="680" y="654" class="uml-method-text">+taoGiaoDichDao(): void</text>
  </g>

  <!-- 9. Số Dư Tồn Kho (StockBalance - Projection) -->
  <g id="class_so_du">
    <rect x="670" y="750" width="220" height="225" class="uml-box" />
    <line x1="670" y1="785" x2="890" y2="785" class="uml-line" />
    <line x1="670" y1="915" x2="890" y2="915" class="uml-line" />

    <text x="780" y="772" class="uml-header-text">Số Dư Tồn Kho</text>

    <text x="680" y="802" class="uml-attr-text">-id: Long</text>
    <text x="680" y="817" class="uml-attr-text">-vat_tu_id: Long</text>
    <text x="680" y="832" class="uml-attr-text">-kho_id: Long</text>
    <text x="680" y="847" class="uml-attr-text">-vi_tri_id: Long</text>
    <text x="680" y="862" class="uml-attr-text">-lo_hang_id: Long?</text>
    <text x="680" y="877" class="uml-attr-text">-so_luong_thuc_te: Double</text>
    <text x="680" y="892" class="uml-attr-text">-so_luong_giu_cho: Double</text>

    <text x="680" y="933" class="uml-method-text">+layTonKhaDung(): Double</text>
    <text x="680" y="949" class="uml-method-text">+capNhatSoDu(delta): void</text>
  </g>

  <!-- 10. Phiếu Nhập Kho (Receipt) -->
  <g id="class_phieu_nhap">
    <rect x="950" y="440" width="220" height="235" class="uml-box" />
    <line x1="950" y1="475" x2="1170" y2="475" class="uml-line" />
    <line x1="950" y1="620" x2="1170" y2="620" class="uml-line" />

    <text x="1060" y="462" class="uml-header-text">Phiếu Nhập Kho</text>

    <text x="960" y="492" class="uml-attr-text">-id: Long</text>
    <text x="960" y="507" class="uml-attr-text">-ma_phieu: String</text>
    <text x="960" y="522" class="uml-attr-text">-kho_id: Long</text>
    <text x="960" y="537" class="uml-attr-text">-loai_nguon: String</text>
    <text x="960" y="552" class="uml-attr-text">-chung_tu_nguon_id: String</text>
    <text x="960" y="567" class="uml-attr-text">-ngay_nhap: Date</text>
    <text x="960" y="582" class="uml-attr-text">-trang_thai: String</text>
    <text x="960" y="597" class="uml-attr-text">-nguoi_nhap_id: Long</text>

    <text x="960" y="638" class="uml-method-text">+themDongNhap(): void</text>
    <text x="960" y="654" class="uml-method-text">+ghiSoNhapKho(): void</text>
  </g>

  <!-- 11. Phiếu Xuất Kho (Issue) -->
  <g id="class_phieu_xuat">
    <rect x="950" y="750" width="220" height="225" class="uml-box" />
    <line x1="950" y1="785" x2="1170" y2="785" class="uml-line" />
    <line x1="950" y1="915" x2="1170" y2="915" class="uml-line" />

    <text x="1060" y="772" class="uml-header-text">Phiếu Xuất Kho</text>

    <text x="960" y="802" class="uml-attr-text">-id: Long</text>
    <text x="960" y="817" class="uml-attr-text">-ma_phieu: String</text>
    <text x="960" y="832" class="uml-attr-text">-kho_id: Long</text>
    <text x="960" y="847" class="uml-attr-text">-ly_do_xuat: String</text>
    <text x="960" y="862" class="uml-attr-text">-module_nguon: String</text>
    <text x="960" y="877" class="uml-attr-text">-chung_tu_nguon_id: String</text>
    <text x="960" y="892" class="uml-attr-text">-trang_thai: String</text>

    <text x="960" y="933" class="uml-method-text">+themDongXuat(): void</text>
    <text x="960" y="949" class="uml-method-text">+ghiSoXuatKho(): void</text>
  </g>


  <!-- ==================== PACKAGE 3 CLASSES (HR & PLATFORM) ==================== -->

  <!-- 12. Nhân Viên (Employee) -->
  <g id="class_nhan_vien">
    <rect x="1250" y="140" width="220" height="275" class="uml-box" />
    <line x1="1250" y1="175" x2="1470" y2="175" class="uml-line" />
    <line x1="1250" y1="355" x2="1470" y2="355" class="uml-line" />

    <text x="1360" y="162" class="uml-header-text">Nhân Viên</text>

    <text x="1260" y="192" class="uml-attr-text">-id: Long</text>
    <text x="1260" y="207" class="uml-attr-text">-ma_nhan_vien: String</text>
    <text x="1260" y="222" class="uml-attr-text">-ho_ten: String</text>
    <text x="1260" y="237" class="uml-attr-text">-ngay_sinh: Date</text>
    <text x="1260" y="252" class="uml-attr-text">-so_dien_thoai: String</text>
    <text x="1260" y="267" class="uml-attr-text">-email: String</text>
    <text x="1260" y="282" class="uml-attr-text">-phong_ban_id: Long</text>
    <text x="1260" y="297" class="uml-attr-text">-chuc_vu_id: Long</text>
    <text x="1260" y="312" class="uml-attr-text">-nguoi_quan_ly_id: Long?</text>
    <text x="1260" y="327" class="uml-attr-text">-trang_thai: String</text>
    <text x="1260" y="342" class="uml-attr-text">-ngay_vao_lam: Date</text>

    <text x="1260" y="373" class="uml-method-text">+tinhThamNien(): Integer</text>
    <text x="1260" y="389" class="uml-method-text">+capNhatThongTin(): void</text>
    <text x="1260" y="405" class="uml-method-text">+voHieuHoa(): void</text>
  </g>

  <!-- 13. Phòng Ban (Department) -->
  <g id="class_phong_ban">
    <rect x="1250" y="715" width="220" height="200" class="uml-box" />
    <line x1="1250" y1="750" x2="1470" y2="750" class="uml-line" />
    <line x1="1250" y1="870" x2="1470" y2="870" class="uml-line" />

    <text x="1360" y="737" class="uml-header-text">Phòng Ban</text>

    <text x="1260" y="767" class="uml-attr-text">-id: Long</text>
    <text x="1260" y="782" class="uml-attr-text">-ma_phong_ban: String</text>
    <text x="1260" y="797" class="uml-attr-text">-ten_phong_ban: String</text>
    <text x="1260" y="812" class="uml-attr-text">-phong_ban_cha_id: Long?</text>
    <text x="1260" y="827" class="uml-attr-text">-trang_thai: String</text>
    <text x="1260" y="842" class="uml-attr-text">-ghi_chu: String</text>

    <text x="1260" y="890" class="uml-method-text">+layCayPhongBan(): List</text>
    <text x="1260" y="906" class="uml-method-text">+themPhongBanCon(): void</text>
  </g>

  <!-- 14. Tài Khoản Người Dùng (UserAccount) -->
  <g id="class_tai_khoan">
    <rect x="1540" y="140" width="190" height="275" class="uml-box" />
    <line x1="1540" y1="175" x2="1730" y2="175" class="uml-line" />
    <line x1="1540" y1="355" x2="1730" y2="355" class="uml-line" />

    <text x="1635" y="162" class="uml-header-text">Tài Khoản Người Dùng</text>

    <text x="1550" y="192" class="uml-attr-text">-id: Long</text>
    <text x="1550" y="207" class="uml-attr-text">-username: String</text>
    <text x="1550" y="222" class="uml-attr-text">-password_hash: String</text>
    <text x="1550" y="237" class="uml-attr-text">-nhan_vien_id: Long?</text>
    <text x="1550" y="252" class="uml-attr-text">-is_super_admin: Bool</text>
    <text x="1550" y="267" class="uml-attr-text">-so_lan_dang_nhap_sai: Int</text>
    <text x="1550" y="282" class="uml-attr-text">-khoa_den: Instant?</text>
    <text x="1550" y="297" class="uml-attr-text">-trang_thai: String</text>
    <text x="1550" y="312" class="uml-attr-text">-ngay_tao: Instant</text>

    <text x="1550" y="373" class="uml-method-text">+xacThuc(matKhau): Bool</text>
    <text x="1550" y="389" class="uml-method-text">+khoaTaiKhoan(): void</text>
    <text x="1550" y="405" class="uml-method-text">+doiMatKhau(): void</text>
  </g>

  <!-- 15. Vai Trò (Role) -->
  <g id="class_vai_tro">
    <rect x="1540" y="480" width="190" height="195" class="uml-box" />
    <line x1="1540" y1="515" x2="1730" y2="515" class="uml-line" />
    <line x1="1540" y1="625" x2="1730" y2="625" class="uml-line" />

    <text x="1635" y="502" class="uml-header-text">Vai Trò (Role)</text>

    <text x="1550" y="532" class="uml-attr-text">-id: Long</text>
    <text x="1550" y="547" class="uml-attr-text">-ma_vai_tro: String</text>
    <text x="1550" y="562" class="uml-attr-text">-ten_hien_thi: String</text>
    <text x="1550" y="577" class="uml-attr-text">-mo_ta: String</text>
    <text x="1550" y="592" class="uml-attr-text">-trang_thai: String</text>

    <text x="1550" y="645" class="uml-method-text">+ganQuyen(quyen): void</text>
    <text x="1550" y="661" class="uml-method-text">+kiemTraQuyen(code): Bool</text>
  </g>

</svg>
"""

svg_path = DOCS_DIR / "class_diagram_vinamik_classic.svg"
svg_path.write_text(svg_content, encoding="utf-8")
print(f"Generated SVG at: {svg_path}")

html_content = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body {{
    margin: 0;
    padding: 24px;
    background: #f8fafc;
    display: flex;
    justify-content: center;
    align-items: center;
  }}
  svg {{
    box-shadow: 0 4px 25px rgba(0,0,0,0.08);
    border: 1px solid #e2e8f0;
  }}
</style>
</head>
<body>
{svg_content}
</body>
</html>
"""

html_path = DOCS_DIR / "class_diagram_vinamik_classic.html"
html_path.write_text(html_content, encoding="utf-8")

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
png_path = DOCS_DIR / "class_diagram_vinamik_classic.png"

cmd = [
    edge_path,
    "--headless",
    "--disable-gpu",
    "--no-sandbox",
    f"--window-size={WIDTH + 60},{HEIGHT + 100}",
    f"--screenshot={png_path}",
    str(html_path)
]

print("Running Edge headless to capture screenshot...")
res = subprocess.run(cmd, capture_output=True, text=True)
if png_path.exists():
    print(f"Generated PNG successfully at: {png_path} (Size: {png_path.stat().st_size} bytes)")
else:
    print(f"Failed to generate PNG. Edge output: {res.stderr}")
