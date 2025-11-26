package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.BookingCreateRequest;
import com.nhahang.restaurant.dto.BookingDTO;
import com.nhahang.restaurant.model.BookingStatus;
import com.nhahang.restaurant.model.TableStatus;
import com.nhahang.restaurant.model.entity.Booking;
import com.nhahang.restaurant.model.entity.RestaurantTable;
import com.nhahang.restaurant.model.entity.User;
import com.nhahang.restaurant.repository.BookingRepository;
import com.nhahang.restaurant.repository.RestaurantTableRepository;
import com.nhahang.restaurant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    /**
     * Tạo đặt bàn mới
     */
    @Transactional
    public Booking createBooking(BookingCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        RestaurantTable table = restaurantTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        if (table.getCapacity() < request.getNumGuests()) {
            throw new RuntimeException("Bàn không đủ chỗ.");
        }

        if (request.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian đặt bàn phải là tương lai");
        }

        LocalDateTime startCheck = request.getBookingTime().minusHours(2);
        LocalDateTime endCheck = request.getBookingTime().plusHours(2);

        boolean isConflict = bookingRepository.existsConflictingBooking(
                request.getTableId(), 
                startCheck, 
                endCheck
        );

        if (isConflict) {
            throw new RuntimeException("Bàn đã được đặt trong khung giờ này (" 
                + request.getBookingTime().minusHours(2).toLocalTime() + " - " 
                + request.getBookingTime().plusHours(2).toLocalTime() + ")");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTable(table);
        booking.setBookingTime(request.getBookingTime());
        booking.setNumGuests(request.getNumGuests());
        booking.setStatus(BookingStatus.Confirmed); 

        Booking savedBooking = bookingRepository.save(booking);
        return savedBooking;
    }

    @Transactional
    public Booking checkInBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt bàn: " + bookingId));

        if (booking.getStatus() != BookingStatus.Confirmed && booking.getStatus() != BookingStatus.Pending) {
            throw new RuntimeException("Chỉ có thể Check-in các đơn đang chờ hoặc đã xác nhận.");
        }
        RestaurantTable table = booking.getTable();
        if (table.getStatus() == TableStatus.Used) {
            throw new RuntimeException("Bàn này hiện đang có người ngồi (Used). Kiểm tra lại thực tế.");
        }
        table.setStatus(TableStatus.Used); 
        restaurantTableRepository.save(table);

        return booking;
    }

    /**
     * Lấy tất cả đặt bàn
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Lấy tất cả đặt bàn với phân trang
     */
    public List<Booking> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookingPage = bookingRepository.findAll(pageable);
        return bookingPage.getContent();
    }

    /**
     * Lấy đặt bàn theo ID
     */
    public Booking getBookingById(Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));
    }

    /**
     * Lấy đặt bàn theo user ID
     */
    public List<Booking> getBookingsByUserId(Integer userId) {
        return bookingRepository.findByUserId(userId);
    }

    /**
     * Lấy đặt bàn theo table ID
     */
    public List<Booking> getBookingsByTableId(Integer tableId) {
        return bookingRepository.findByTableId(tableId);
    }
    /**
     * Lấy đặt bàn theo số điện thoại
     */
    public List<Booking> getBookingsByPhoneNumber(String phoneNumber) {
        return bookingRepository.findByUserPhoneNumber(phoneNumber);
    }

    /**
     * Cập nhật trạng thái đặt bàn
     */
    @Transactional
    public Booking updateBookingStatus(Integer id, String statusStr) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        BookingStatus status;
        try {
            status = BookingStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + statusStr);
        }

        booking.setStatus(status);

        RestaurantTable table = booking.getTable();
        if (status == BookingStatus.Cancelled) {
            table.setStatus(TableStatus.Available);
        } else if (status == BookingStatus.Confirmed) {
            table.setStatus(TableStatus.Booked);
        } else if (status == BookingStatus.Completed) {
            table.setStatus(TableStatus.Available);
        }
        restaurantTableRepository.save(table);

        return bookingRepository.save(booking);
    }

    /**
     * Hủy đặt bàn
     */
    @Transactional
    public Booking cancelBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        if (booking.getStatus() == BookingStatus.Cancelled) {
            throw new RuntimeException("Đặt bàn đã được hủy trước đó");
        }

        if (booking.getStatus() == BookingStatus.Completed) {
            throw new RuntimeException("Không thể hủy đặt bàn đã hoàn thành");
        }

        booking.setStatus(BookingStatus.Cancelled);
        RestaurantTable table = booking.getTable();
        table.setStatus(TableStatus.Available);
        restaurantTableRepository.save(table);

        return bookingRepository.save(booking);
    }

    /**
     * Hoàn thành đặt bàn
     */
    @Transactional
    public Booking completeBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        if (booking.getStatus() == BookingStatus.Cancelled) {
            throw new RuntimeException("Không thể hoàn thành đặt bàn đã bị hủy");
        }

        if (booking.getStatus() == BookingStatus.Completed) {
            throw new RuntimeException("Đặt bàn đã được hoàn thành trước đó");
        }

        booking.setStatus(BookingStatus.Completed);

        RestaurantTable table = booking.getTable();
        table.setStatus(TableStatus.Booked);
        restaurantTableRepository.save(table);

        return bookingRepository.save(booking);
    }

    /**
     * Cập nhật thông tin đặt bàn
     */
    @Transactional
    public Booking updateBooking(Integer id, BookingCreateRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        if (booking.getStatus() == BookingStatus.Cancelled) {
            throw new RuntimeException("Không thể cập nhật đặt bàn đã bị hủy");
        }

        if (booking.getStatus() == BookingStatus.Completed) {
            throw new RuntimeException("Không thể cập nhật đặt bàn đã hoàn thành");
        }

        if (!booking.getTable().getId().equals(request.getTableId())) {
            RestaurantTable oldTable = booking.getTable();
            oldTable.setStatus(TableStatus.Available);
            restaurantTableRepository.save(oldTable);

            RestaurantTable newTable = restaurantTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));

            if (newTable.getCapacity() < request.getNumGuests()) {
                throw new RuntimeException("Bàn mới không đủ chỗ");
            }

            if (newTable.getStatus() != TableStatus.Available) {
                throw new RuntimeException("Bàn mới không khả dụng");
            }

            booking.setTable(newTable);
            newTable.setStatus(TableStatus.Booked);
            restaurantTableRepository.save(newTable);
        }

        booking.setBookingTime(request.getBookingTime());
        booking.setNumGuests(request.getNumGuests());

        return bookingRepository.save(booking);
    }

    /**
     * Xóa đặt bàn
     */
    @Transactional
    public void deleteBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        RestaurantTable table = booking.getTable();
        table.setStatus(TableStatus.Available);
        restaurantTableRepository.save(table);

        bookingRepository.delete(booking);
    }
}
