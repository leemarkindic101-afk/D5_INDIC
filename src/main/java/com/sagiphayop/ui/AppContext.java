package com.sagiphayop.ui;

import com.sagiphayop.dao.ActivityLogDao;
import com.sagiphayop.dao.AdoptionDao;
import com.sagiphayop.dao.AnimalDao;
import com.sagiphayop.dao.BarangayDao;
import com.sagiphayop.dao.OwnerDao;
import com.sagiphayop.dao.RescueDao;
import com.sagiphayop.dao.UserDao;
import com.sagiphayop.dao.VaccinationDao;

import java.util.Scanner;

/** Everything the menus share: one instance of each DAO plus the console scanner. */
public record AppContext(UserDao userDao, ActivityLogDao activityLogDao, BarangayDao barangayDao,
                         OwnerDao ownerDao, AnimalDao animalDao, VaccinationDao vaccinationDao,
                         RescueDao rescueDao, AdoptionDao adoptionDao, Scanner scanner) { }
