//package com.doc;
//
//import java.util.*;
//
//public class Test {
//    public static void main(String[] args) {
//        List<Employee> employees = Arrays.asList(
//                new Employee(1, "Alice", 5000, "HR"),
//                new Employee(2, "Bob", 6000, "IT"),
//                new Employee(3, "Charlie", 5500, "HR"),
//                new Employee(4, "David", 7000, "IT"),
//                new Employee(5, "Eve", 6500, "Finance")
//        );
//
//        Map<String, Double> totalSalaryByDept = new HashMap<>();
//
//        Map<String,Integer> count = new HashMap<>();
//
//
//        for (Employee e : employees)
//        {
//                totalSalaryByDept.put(e.department(),totalSalaryByDept.getOrDefault(e.department(),0.0)+e.salary());
//
//                count.put(e.department(),count.getOrDefault(e.department(),0)+1);
//
//        }
//
//        Map<String, Double> avgSalaryByDept = new HashMap<>();
//
//        for (String dept: totalSalaryByDept.keySet())
//        {
//
//            double total= totalSalaryByDept.get(dept);
//            int count1 = count.get(dept) ;
//
//            avgSalaryByDept.put(dept,total/count1);
//
//
//
//        }
//        System.out.println(avgSalaryByDept);
//
//
//    }
//}
//
//record Employee(int id, String name, double salary, String department) {}
