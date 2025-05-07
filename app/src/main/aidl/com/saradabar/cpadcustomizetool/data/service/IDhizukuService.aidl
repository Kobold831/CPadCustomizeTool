package com.saradabar.cpadcustomizetool.data.service;

interface IDhizukuService {
    boolean tryInstallPackages(in java.util.List<String> installData, int reqCode) = 25;
}
