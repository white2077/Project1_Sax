package com.sax.views.quanly.viewmodel;

import com.sax.dtos.DonHangDTO;
import com.sax.utils.CurrencyConverter;
import lombok.Data;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Data
public class DonHangViewObject extends AbstractViewObject {
    private int maNV;
    private String tongTien;
    private LocalDateTime ngayTao;
    private boolean pttt;
    private String tienHang;
    private String chietKhau;

    public DonHangViewObject(DonHangDTO donHangDTO) {
        super(donHangDTO.getId(), donHangDTO.getKhach().getTenKhach());
        maNV = donHangDTO.getAccount().getId();
        tongTien = CurrencyConverter.parseString(donHangDTO.getTongTien());
        tienHang = CurrencyConverter.parseString(donHangDTO.getTienHang());
        chietKhau = CurrencyConverter.parseString(donHangDTO.getChietKhau());
        ngayTao = donHangDTO.getNgayTao();
        pttt = donHangDTO.getPttt();
    }

    @Override
    public Object[] toObject(ExecutorService executorService, JTable tbl, Set tempIdSet, List<JCheckBox> setCbk) {
        setCbk.add(checkBoxDelete);
        checkBoxDelete.addActionListener((e) -> {
            if (checkBoxDelete.isSelected()) tempIdSet.add(id);
            else tempIdSet.remove(id);
        });
        return new Object[]{checkBoxDelete, id, name, maNV, tienHang, chietKhau, tongTien, pttt ? "Tiền mặt" : "Chuyển khoản", ngayTao};
    }
    
    public Object[] toObject()
    {
        return new Object[] {checkBoxDelete, id, name, tienHang, chietKhau, tongTien, pttt ? "Tiền mặt" : "Chuyển khoản"};
    }
}