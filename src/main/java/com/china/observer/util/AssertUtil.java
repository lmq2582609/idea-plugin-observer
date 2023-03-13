package com.china.observer.util;

public class AssertUtil {

//    public static void isBlank(String str, Integer code, String message) {
//        if (StrUtil.isBlank(str)) {
//            throw new BusinessException(code, message);
//        }
//    }
//
//    public static void isBlank(String str, GlobalResultCode resultCode) {
//        if (StrUtil.isBlank(str)) {
//            throw new BusinessException(resultCode);
//        }
//    }

    /**
     * 为空时，不抛出异常，直接return
     * @param obj
     */
    public static void isNullNoException(Object obj) {
        if (obj == null) {
            return;
        }
    }

}
