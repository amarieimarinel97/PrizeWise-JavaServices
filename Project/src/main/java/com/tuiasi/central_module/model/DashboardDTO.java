package com.tuiasi.central_module.model;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private List<String> portfolio;
    private List<String> history;
    private List<String> watchList;
}



