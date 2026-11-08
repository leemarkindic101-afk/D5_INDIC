package com.sagiphayop;

import com.sagiphayop.dao.ActivityLogDao;
import com.sagiphayop.dao.AnimalDao;
import com.sagiphayop.dao.DatabaseInitializer;
import com.sagiphayop.dao.UserDao;
import com.sagiphayop.dao.VaccinationDao;
import com.sagiphayop.ui.Authentication;

import java.util.Scanner;

/** Startup entry point only: init DB -> create shared instances -> hand off to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserDao userDao = new UserDao();
        ActivityLogDao activityLogDao = new ActivityLogDao();
        AnimalDao animalDao = new AnimalDao();
        VaccinationDao vaccinationDao = new VaccinationDao();

        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(userDao, activityLogDao, animalDao, vaccinationDao, scanner).start();
        }
    }
}
