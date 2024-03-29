package com.sax.views.quanly.viewmodel;

import com.sax.dtos.CtkmDTO;
import com.sax.dtos.CtkmSachDTO;
import com.sax.dtos.SachDTO;
import com.sax.utils.CurrencyConverter;
import com.sax.views.components.table.CellNameRender;
import lombok.Data;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Data
public class CtkmSachViewObject extends AbstractViewObject {
    private SachDTO sach;
    private CtkmDTO ctkm;
    private Long giaTriGiam;

    public CtkmSachViewObject(CtkmSachDTO ctkmSachDTO) {
        super(ctkmSachDTO.getId(), ctkmSachDTO.getSach().getTenSach());
        sach = ctkmSachDTO.getSach();
        ctkm = ctkmSachDTO.getCtkm();
        giaTriGiam = ctkmSachDTO.getGiaTriGiam();
    }

    public String getTrangThai() {
        if (LocalDateTime.now().isAfter(ctkm.getNgayBatDau()) && LocalDateTime.now().isBefore(ctkm.getNgayKetThuc()))
            return "Đang diễn ra";
        else if (LocalDateTime.now().isAfter(ctkm.getNgayKetThuc())) return "Đã kết thúc";
        return "Chưa bắt đầu";
    }

    @Override
    public Object[] toObject(ExecutorService executorService, JTable tbl, Set tempIdSet, List<JCheckBox> setCbk) {
        setCbk.add(checkBoxDelete);
        checkBoxDelete.addActionListener((e) -> {
            if (checkBoxDelete.isSelected()) tempIdSet.add(id);
            else tempIdSet.remove(id);
        });
        return new Object[]{checkBoxDelete, id, new CellNameRender(executorService, tbl, sach.getHinhAnh(), name), ctkm.getTenSuKien(), ctkm.getNgayBatDau(), ctkm.getNgayKetThuc(), ctkm.isKieuGiamGia() ? giaTriGiam + "%" : CurrencyConverter.parseString(giaTriGiam), getTrangThai()};
    }

    @Override
    public Object[] toObject() {
        return new Object[0];
    }
}