# App Bán Hàng (Android)

## 📌 Giới thiệu chức năng

Ứng dụng **App Bán Hàng** hỗ trợ quản lý bán hàng và nhà hàng nhỏ. Các chức năng chính:

### 🛍️ 1. Quản lý sản phẩm
- Hiển thị danh sách sản phẩm với hình ảnh, mô tả và giá.
- Thêm sản phẩm vào giỏ hàng nhanh chóng.

### 🛒 2. Giỏ hàng
- Xem danh sách sản phẩm đã chọn.
- Tăng/giảm số lượng sản phẩm trong giỏ.
- Tính tổng tiền tự động.
- Chọn phương thức thanh toán (tiền mặt, chuyển khoản).

### 📦 3. Quản trị đơn hàng (Admin)
- **Đơn hàng chờ**: xem, xác nhận hoặc hủy đơn.
- **Đơn đã xác nhận**: quản lý đơn đã duyệt, hỗ trợ thanh toán và in hóa đơn.
- **Quản lý bàn**: hiển thị trạng thái bàn (trống/đang phục vụ).

### 🌐 4. Kết nối mạng
- Hoạt động với dữ liệu online qua Firebase/Firestore.
- Phát hiện khi **mất mạng**, chuyển đến màn hình Offline và cho phép **thử lại**.

### 🔑 5. Đăng nhập & phân quyền
- Đăng nhập bằng Firebase Authentication.
- Phân quyền:
  - **Người dùng**: đặt hàng, quản lý giỏ hàng.
  - **Nhân viên**: quản lý đơn hàng, bàn và xác nhận thanh toán.
