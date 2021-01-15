/*
 * Copyright (C) 2021 dreamn(dream@dreamn.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.dreamn.qianji_auto.core.utils;

import cn.dreamn.qianji_auto.core.db.AutoBill;
import cn.dreamn.qianji_auto.core.db.DbManger;

public class AutoBills {
    public static AutoBill[] getAll(){
        AutoBill[] autoBills= DbManger.db.AutoBillDao().getAll();
        if(autoBills.length<=0)return null;
        return autoBills;
    }

    public  static void del(int id){
        DbManger.db.AutoBillDao().del(id);
    }
    public static void add(BillInfo billInfo){
        DbManger.db.AutoBillDao().add(billInfo.getType(),billInfo.getMoney(),billInfo.getTime(),billInfo.getRemark(),billInfo.getCateName(),billInfo.getCateChoose()?"1":"0",billInfo.getBookName(),billInfo.getAccountName(),billInfo.getAccountName2(),billInfo.getShopAccount(),billInfo.getShopRemark());
    }
    public static void update(int id,BillInfo billInfo){
        DbManger.db.AutoBillDao().update(id,billInfo.getType(),billInfo.getMoney(),billInfo.getTime(),billInfo.getRemark(),billInfo.getCateName(),billInfo.getCateChoose()?"1":"0",billInfo.getBookName(),billInfo.getAccountName(),billInfo.getAccountName2(),billInfo.getShopAccount(),billInfo.getShopRemark());
    }

}