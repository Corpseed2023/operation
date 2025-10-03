package com.doc;

import java.util.*;

public class Test {

    public static void main(String[] args) {


        int [] nums = {0,0,0,1,2,2,4,4,6,6,7};

        Map<Integer,Integer> map = new HashMap<>();
        List<Integer> result = new ArrayList<>();


        for (int i : nums)
        {
            if (!map.containsKey(i))
            {
                map.put(i,1);
                result.add(i);
            }
        }
        for (int num : result) {
            System.out.println(num + " appears " + map.get(num) + " times");
        }





    }



}
