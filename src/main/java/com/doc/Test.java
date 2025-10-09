package com.doc;

public class Test {

    public static void main(String[] args) {

        int k = 1;
        int[] nums = {1, 1, 2,2,2,3};

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] != nums[i - 1]) {
                k++;
            }
        }

        System.out.println("Number of duplicates: " + k);
    }
}
