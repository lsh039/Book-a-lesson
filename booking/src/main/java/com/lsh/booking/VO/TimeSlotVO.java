package com.lsh.booking.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotVO {
    private String time;//时间段

    private boolean available;//是否可以预约

}
