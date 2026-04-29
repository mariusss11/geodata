//package com.geodata.utils;
//
//import com.geodata.model.Borrows;
//
//public class BorrowMapper {
//
//    private BorrowMapper() {
//        throw new UnsupportedOperationException("Utility class");
//    }
//
//    public static BorrowsDTO toDto(Borrows borrow, LibraryItem itemDTO, Client client) {
//        return BorrowsDTO.builder()
//                .id(borrow.getId())
//                .status(borrow.getStatus().toLowerCase())
//                .borrowDate(borrow.getBorrowDate())
//                .returnDate(borrow.getReturnDate())
//                .client(client)
//                .item(itemDTO)
//                .build();
//    }
//
//    public static BorrowsDTO toDto(Borrows borrow, LibraryItem itemDTO) {
//        return BorrowsDTO.builder()
//                .id(borrow.getId())
//                .status(borrow.getStatus().toLowerCase())
//                .borrowDate(borrow.getBorrowDate())
//                .returnDate(borrow.getReturnDate())
//                .client(null)
//                .item(itemDTO)
//                .build();
//    }
//}
