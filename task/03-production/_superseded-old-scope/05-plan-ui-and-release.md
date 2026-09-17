# PROD-05 — Giao diện kế hoạch và phát hành
- Chức năng: 2/5 — Lập kế hoạch sản xuất
- Trạng thái: TODO
- Phụ thuộc: `04-plan-model-api.md`
## Mục tiêu
Planner chỉnh kế hoạch và phát hành khối lượng được duyệt thành lệnh sản xuất.
## Phạm vi
Danh sách kế hoạch, form sửa draft, xem kết quả MRP liên quan và action release.
## Tiêu chí nghiệm thu
- Plan released không bị sửa ngầm như draft.
- User chỉ xem không có action release.
- Phát hành lặp không tạo lệnh trùng.
## Kiểm tra
Thử draft, release, duplicate request và role chỉ xem.
## Ngoài phạm vi
Không thực hiện chấm công hoặc nhập vật tư tại màn hình lập kế hoạch.

