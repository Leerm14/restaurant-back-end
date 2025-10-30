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
    public BookingDTO createBooking(BookingCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + request.getUserId()));

        RestaurantTable table = restaurantTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));

        if (table.getCapacity() < request.getNumGuests()) {
            throw new RuntimeException("Bàn không đủ chỗ. Sức chứa: " + table.getCapacity() + 
                    ", Số khách: " + request.getNumGuests());
        }
        if (table.getStatus() != TableStatus.Available) {
            throw new RuntimeException("Bàn hiện không khả dụng");
        }
        if (request.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian đặt bàn phải là thời gian trong tương lai");
        }

        List<Booking> existingBookings = bookingRepository.findByTableId(request.getTableId());
        for (Booking existing : existingBookings) {
            if (existing.getStatus() == BookingStatus.Confirmed) {
                LocalDateTime existingTime = existing.getBookingTime();
                if (Math.abs(java.time.Duration.between(existingTime, request.getBookingTime()).toHours()) < 2) {
                    throw new RuntimeException("Bàn đã được đặt trong khung giờ này");
                }
            }
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTable(table);
        booking.setBookingTime(request.getBookingTime());
        booking.setNumGuests(request.getNumGuests());
        booking.setStatus(BookingStatus.Confirmed);

        Booking savedBooking = bookingRepository.save(booking);

        table.setStatus(TableStatus.Booked);
        restaurantTableRepository.save(table);

        return convertToDTO(savedBooking);
    }

    /**
     * Lấy tất cả đặt bàn
     */
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đặt bàn theo ID
     */
    public BookingDTO getBookingById(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));
        return convertToDTO(booking);
    }

    /**
     * Lấy đặt bàn theo user ID
     */
    public List<BookingDTO> getBookingsByUserId(Integer userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đặt bàn theo table ID
     */
    public List<BookingDTO> getBookingsByTableId(Integer tableId) {
        return bookingRepository.findByTableId(tableId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Hủy đặt bàn
     */
    @Transactional
    public BookingDTO cancelBooking(Integer id) {
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

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToDTO(updatedBooking);
    }

    /**
     * Hoàn thành đặt bàn
     */
    @Transactional
    public BookingDTO completeBooking(Integer id) {
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

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToDTO(updatedBooking);
    }

    /**
     * Cập nhật thông tin đặt bàn
     */
    @Transactional
    public BookingDTO updateBooking(Integer id, BookingCreateRequest request) {
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

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToDTO(updatedBooking);
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

    /**
     * Chuyển đổi Entity sang DTO
     */
    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getFullName());
        dto.setTableId(booking.getTable().getId());
        dto.setTableName("Bàn " + booking.getTable().getTableNumber());
        dto.setBookingTime(booking.getBookingTime());
        dto.setNumGuests(booking.getNumGuests());
        dto.setStatus(booking.getStatus());
        return dto;
    }
}
