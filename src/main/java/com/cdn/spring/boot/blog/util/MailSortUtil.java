package com.cdn.spring.boot.blog.util;


import com.cdn.spring.boot.blog.domain.MailList;
import com.cdn.spring.boot.blog.domain.view.MailListView;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.*;

/**
 * @description 根据好友列表的备注和昵称排序, 有备注按照备注排序->没有备注按照昵称排序
 **/
public class MailSortUtil {

    private MailSortUtil(){

    }

    private final  static char[] chars =new char[]{'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'};


    /**
     * 获取首字母
     */
    public static char ChineseToFirstLetter(char c){
            String d = String.valueOf(c);
            String str = converterToFirstSpell(d);
            String s = str.toUpperCase();
            return s.charAt(0);
    }

    /**
     *中文转首字母
     */
    public static String ChineseToFirstLetter(String c) {
        String string = "";
        char b;
        int a = c.length();
        for (int k = 0; k < a; k++) {
            b = c.charAt(k);
            String d = String.valueOf(b);
            String str = converterToFirstSpell(d);
            String s = str.toUpperCase();
            String g = s;
            char h;
            int j = g.length();
            for (int y = 0; y <= 0; y++) {
                h = g.charAt(0);
                string += h;
            }
        }
        return string;
    }


    private static String converterToFirstSpell(String chines) {
        String pinyinName = "";
        char[] nameChar = chines.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < nameChar.length; i++) {
            String s = String.valueOf(nameChar[i]);
            if (s.matches("[\\u4e00-\\u9fa5]")) {
                try {
                    String[] mPinyinArray = PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat);
                    pinyinName += mPinyinArray[0];
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinName += nameChar[i];
            }
        }
        return pinyinName;
    }

    /**
     * @param list  未排序的通讯录好友
     *               备注权重大于昵称
     * @return      根据昵称或备注按照搜字母大写进行分类排序
     */
    public static LinkedHashMap<String,ArrayList<MailListView>> MailListOrders(List<MailList> list){
            //先统计出每个字母有多少个好友,然后再根据26个字母 循环往linkHashMap里面放
            HashMap<String,ArrayList<MailListView>>  mailListmap=new HashMap<>();
            LinkedHashMap<String,ArrayList<MailListView>>  linkedListMap=new  LinkedHashMap<>();
            String name=null;
            //排序
            mailListMapOrder(mailListmap,linkedListMap);
            return linkedListMap;
    }

    /**
     * 检测首位是否包含在26字母里
     * @param c
     * @return
     */
    private static boolean contains(char c){
        for(int i=0;i<chars.length;i++){
            if(chars[i]==c){
                return  true;
            }
        }
        return false;
    }

    /**
     * 往map里面放
     */
    private static void  putMap(HashMap<String,ArrayList<MailListView>> map,String name,MailList mailList){
         char c=ChineseToFirstLetter(name.charAt(0));
         if(!contains(c)){
             c='#';
         }
         String key=String.valueOf(c);
         if(map.get(key)==null){//不存在就new 一个出来
             ArrayList<MailListView> list=new ArrayList<>();
             list.add(new MailListView(mailList));
             map.put(key,list);
         }else{
             ArrayList<MailListView> list=map.get(key);
             list.add(new MailListView(mailList));
         }
    }

    /**
     * 根据分好类的map进行排序
     * return 排序好的通讯录好友信息
     */
    private static  void  mailListMapOrder(HashMap<String,ArrayList<MailListView>>  mailListmap,LinkedHashMap<String,ArrayList<MailListView>>  linkedListMap){
        for(int i=0;i<chars.length;i++){
            String key=String.valueOf(chars[i]);
             if(mailListmap.get(key)!=null){
                 linkedListMap.put(key,mailListmap.get(key));
             }
        }
        //把其它的放到 # 组里面
        if(mailListmap.get("#")!=null){
            linkedListMap.put("#",mailListmap.get("#"));
        }
    }


    public static void main(String[] args) {
    }

}
