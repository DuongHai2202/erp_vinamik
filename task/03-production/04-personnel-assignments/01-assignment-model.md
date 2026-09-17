# PROD-14 — Mô hình phân công nhân sự
- Nhóm chức năng: 4/5 — Quản lý phân công nhân sự
- Nguồn: ERP_ht FR26–FR33
- Trạng thái: IN_PROGRESS
- Phụ thuộc: Production order model, HR employee lookup contract

## Mục tiêu
Gắn nhân viên vào lệnh/công đoạn/ca trong khoảng thời gian xác định.

## Phạm vi
Tạo production.production_assignment gồm order ID nội bộ; employee ID và work shift ID tham chiếu ngoài từ HR contract; công việc/công đoạn, thời gian bắt đầu/kết thúc và trạng thái. Xác thực nhân viên/ca đang dùng qua public contract, không sao chép employee hoặc shift master vào Production.

## Tiêu chí nghiệm thu
- Nhân viên tra cứu qua HR contract; Production không tạo bảng hồ sơ riêng.
- Thời gian hợp lệ và hỗ trợ phát hiện lịch phân công chồng lấn.
- Hủy assignment giữ lịch sử nếu đã dùng để ghi nhận thực tế.

## Kiểm tra
Test quan hệ, khoảng thời gian và trạng thái assignment.

## Ngoài phạm vi
Không quản lý hồ sơ HR, chấm công hoặc tính lương.

