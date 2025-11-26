package com.doc;

import java.util.*;

public class Test {

    public static List<String> palindrome(String value) {

        StringBuilder s1 = new StringBuilder(value);

        s1.reverse();
        List list = new ArrayList<>();

        if (value.equals(s1.toString()))

            list.add(value);
            System.out.println("palindrome");
            return list;
    }


    public static void main(String[] args) {

        String a ="abaddddaaa";

        for (int i =0 ; i<a.length();i++)
        {

            for (int j = i; j<a.length();j++)
            {

                String test = a.substring(i,j+1);

                palindrome(test);

            }
        }



    }
}

