package com.doc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Test {


    public static void main(String[] args) {


//        List<Integer> list = Arrays.asList(7,7,4,3,8,9,1,1);

        List<String> list = Arrays.asList("Java","hello", "aest");


        List<String> finalNumber = list.stream().sorted().collect(Collectors.toList());

        System.out.println(finalNumber);






        }




}
