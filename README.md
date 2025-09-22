# 🍽️ App Bán Hàng - Quản Lý Quán Ăn

Ứng dụng Android hỗ trợ quản lý bán hàng và nhà hàng nhỏ, phục vụ cho cả **khách hàng**, **nhân viên** và **chủ quán**.  
Dữ liệu được đồng bộ thời gian thực với **Firebase Firestore** và xác thực qua **Firebase Authentication**.

---

## 📌 Giới thiệu chức năng

### 🛍️ 1. Quản lý sản phẩm
- Hiển thị danh sách sản phẩm với hình ảnh, mô tả và giá.
- Thêm sản phẩm vào giỏ hàng nhanh chóng.
- Chủ quán có thể thêm, sửa, xóa sản phẩm và tìm kiếm theo tên/loại.

### 🛒 2. Giỏ hàng (Người dùng)
- Xem danh sách sản phẩm đã chọn.
- Tăng/giảm số lượng sản phẩm trong giỏ 
- Tự động tính tổng tiền.
- Chọn phương thức thanh toán (tiền mặt hoặc chuyển khoản).
- Gửi đơn đặt hàng cho Admin.

### 📦 3. Quản trị đơn hàng (Admin)
- **Đơn chờ:** xem, xác nhận hoặc hủy đơn.
- **Đơn đã xác nhận:** quản lý, đánh dấu thanh toán, hỗ trợ in hóa đơn PDF.
- Quản lý bàn: thêm, sửa, xóa bàn và theo dõi trạng thái (trống/đang phục vụ).
- Nhận và xử lý yêu cầu gọi nhân viên từ khách hàng.

### 📊 4. Thống kê (Chủ quán)
- Xem doanh thu theo ngày, tuần, tháng.
- Quản lý tổng quan số lượng đơn, số lượng bàn, và doanh thu.

### 🌐 5. Kết nối mạng
- Hoạt động online với dữ liệu trên Firestore.
- Khi mất mạng: tự động chuyển đến màn hình Offline và cho phép thử lại.

### 🔑 6. Đăng nhập & phân quyền
- Đăng nhập bằng Firebase Authentication.
- **Người dùng:** đặt món, quản lý giỏ hàng.
- **Nhân viên/Admin:** quản lý đơn hàng, bàn, xác nhận thanh toán, in hóa đơn.
- **Chủ quán:** quản lý sản phẩm, bàn, doanh thu.

---

## 📱 Cách dùng

### 👤 Người dùng (Khách hàng)
1. Đăng nhập → xem menu sản phẩm.
2. Thêm món vào giỏ → chỉnh số lượng → chọn phương thức thanh toán.
3. Gửi đơn đặt hàng → chờ nhân viên xác nhận.
4. Có thể gọi nhân viên trực tiếp từ màn hình chính.

### 🧑‍🍳 Nhân viên / Admin
1. Đăng nhập vào giao diện Admin.
2. Quản lý đơn hàng:
   - Xem danh sách đơn chờ, xác nhận hoặc hủy.
   - Xem đơn đã xác nhận, đánh dấu thanh toán, in hóa đơn.
3. Quản lý bàn và nhận yêu cầu gọi nhân viên từ khách.

### 👑 Chủ quán
1. Vào giao diện Chủ quán.
2. Quản lý sản phẩm: thêm/sửa/xóa, tìm kiếm theo tên hoặc loại.
3. Quản lý bàn ăn.
4. Xem báo cáo doanh thu theo ngày/tuần/tháng.

---

👉 Ứng dụng hướng đến các quán ăn/quán cafe nhỏ, dễ triển khai, dễ dùng, và cập nhật thời gian thực qua Firebase.
