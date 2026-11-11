package com.sagiphayop;

import com.sagiphayop.dao.ActivityLogDao;
import com.sagiphayop.dao.AdoptionDao;
import com.sagiphayop.dao.AnimalDao;
import com.sagiphayop.dao.BarangayDao;
import com.sagiphayop.dao.DatabaseInitializer;
import com.sagiphayop.dao.OwnerDao;
import com.sagiphayop.dao.RescueDao;
import com.sagiphayop.dao.UserDao;
import com.sagiphayop.dao.VaccinationDao;
import com.sagiphayop.ui.AppContext;
import com.sagiphayop.ui.Authentication;

import java.util.Scanner;

/** Startup entry point only: init DB -> create shared instances -> hand off to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try (Scanner scanner = new Scanner(System.in)) {
            AppContext ctx = new AppContext(new UserDao(), new ActivityLogDao(), new BarangayDao(),
                    new OwnerDao(), new AnimalDao(), new VaccinationDao(), new RescueDao(),
                    new AdoptionDao(), scanner);
            new Authentication(ctx).start();
        }
    }
}
