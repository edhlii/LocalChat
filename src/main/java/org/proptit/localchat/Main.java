package org.proptit.localchat;

import org.proptit.localchat.client.ClientRun;
import org.proptit.localchat.server.ServerRun; // Giả sử class chạy Server của bạn tên là ServerRun

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========== HỆ THỐNG LOCALCHAT ==========");
        System.out.println("1. Khởi động Server (Dành cho Máy chủ)");
        System.out.println("2. Khởi động Client (Dành cho Người dùng)");
        System.out.println("========================================");
        System.out.print("Vui lòng chọn chế độ chạy (1 hoặc 2): ");

        int choice = 0;
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ!");
            System.exit(0);
        }

        if (choice == 1) {
            System.out.println("Đang khởi động Server...");
            ServerRun.main(args);
        } else if (choice == 2) {
            System.out.println("Đang khởi động Client...");
            ClientRun.main(args);
        } else {
            System.out.println("Chỉ được nhập 1 hoặc 2. Chương trình kết thúc.");
        }
    }
}