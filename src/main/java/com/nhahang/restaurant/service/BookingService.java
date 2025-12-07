package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.BookingCreateRequest;
import com.nhahang.restaurant.model.BookingStatus;
import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.TableStatus;
import com.nhahang.restaurant.model.entity.Booking;
import com.nhahang.restaurant.model.entity.RestaurantTable;
import com.nhahang.restaurant.model.entity.User;
import com.nhahang.restaurant.repository.BookingRepository;
import com.nhahang.restaurant.repository.OrderRepository;
import com.nhahang.restaurant.repository.RestaurantTableRepository;
import com.nhahang.restaurant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;

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
            throw new RuntimeException("Bàn đã được đặt trong khung giờ này");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTable(table);
        booking.setBookingTime(request.getBookingTime());
        booking.setNumGuests(request.getNumGuests());
        booking.setStatus(BookingStatus.Confirmed); 

        return bookingRepository.save(booking);
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
            throw new RuntimeException("Bàn này hiện đang có người ngồi.");
        }
        table.setStatus(TableStatus.Used); 
        restaurantTableRepository.save(table);

        return booking;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));
    }

    public List<Booking> getBookingsByUserId(Integer userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getBookingsByTableId(Integer tableId) {
        return bookingRepository.findByTableId(tableId);
    }

    public List<Booking> getBookingsByPhoneNumber(String phoneNumber) {
        return bookingRepository.findByUserPhoneNumber(phoneNumber);
    }

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

    @Transactional
    public void deleteBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        RestaurantTable table = booking.getTable();
        table.setStatus(TableStatus.Available);
        restaurantTableRepository.save(table);

        bookingRepository.delete(booking);
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void autoCancelOverdueBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<BookingStatus> targetStatuses = Arrays.asList(BookingStatus.Pending, BookingStatus.Confirmed);
        
        List<OrderStatus> activeOrderStatuses = Arrays.asList(
            OrderStatus.Pending, 
            OrderStatus.Confirmed, 
            OrderStatus.Preparing
        );

        List<Booking> overdueBookings = bookingRepository.findOverdueBookings(targetStatuses, threshold);

        for (Booking booking : overdueBookings) {
            RestaurantTable table = booking.getTable();
            
            boolean hasActiveOrder = false;
            if (table != null) {
                hasActiveOrder = orderRepository.existsActiveOrderAtTable(table.getId(), activeOrderStatuses);
            }

            if (hasActiveOrder) {
                booking.setStatus(BookingStatus.Completed);
            } else {
                booking.setStatus(BookingStatus.Cancelled);
                if (table != null && table.getStatus() != TableStatus.Used) {
                    table.setStatus(TableStatus.Available);
                    restaurantTableRepository.save(table);
                }
            }
        }
        
        bookingRepository.saveAll(overdueBookings);
    }
}