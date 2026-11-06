package com.sagiphayop;

import com.sagiphayop.dao.DatabaseInitializer;
import com.sagiphayop.dao.UserDao;
import com.sagiphayop.ui.Authentication;

import java.util.Scanner;

/** Startup entry point only: init DB -> create shared instances -> hand off to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserDao userDao = new UserDao();
        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(userDao, scanner).start();
        }
    }
}
