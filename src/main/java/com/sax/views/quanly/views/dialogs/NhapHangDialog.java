package com.sax.views.quanly.views.dialogs;

import com.sax.dtos.LichSuNhapHangDTO;
import com.sax.dtos.SachDTO;
import com.sax.services.ILichSuNhapHangService;
import com.sax.services.ISachService;
import com.sax.utils.ContextUtils;
import com.sax.utils.MsgBox;
import com.sax.views.components.Search;
import com.sax.views.components.libraries.ButtonToolItem;
import com.sax.views.components.libraries.RoundPanel;
import com.sax.views.quanly.viewmodel.SachViewObject;
import com.sax.views.quanly.views.panes.SanPhamPane;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;

public class NhapHangDialog extends JDialog {
    private JPanel contentPane;
    private JTextField txtGiaNhap;
    private JXDatePicker dateNgayNhap;
    private JTextField txtSL;
    private JButton btnSave;
    private JPanel donHangPanel;
    private JPanel bg;
    private JXTable tbl;
    private Search timKiem;
    private JCheckBox cbkSelectedAll;
    private JButton btnEdit;
    private JComboBox comboBox1;
    private DefaultTableModel tableModel;
    private ILichSuNhapHangService lichSuNhapHangService;
    private ISachService sachService;

    public SanPhamPane parentPane;
    public int id;

    public NhapHangDialog() {
        btnSave.addActionListener(e -> {
            LichSuNhapHangDTO lichSuNH = readForm();
            if (lichSuNH != null) {
                try {
                    SachDTO sachDTO = sachService.getById(id);
                    sachDTO.setSoLuong(sachDTO.getSoLuong() + lichSuNH.getSoLuong());
                    sachService.update(sachDTO);

                    lichSuNH.setSach(sachDTO);
                    lichSuNhapHangService.insert(lichSuNH);
                    parentPane.fillTable(sachService.getAll().stream().map(SachViewObject::new).collect(Collectors.toList()));
                    dispose();
                } catch (Exception ex) {
                    MsgBox.alert(null, ex.getMessage());
                }
            }
        });
        initComponent();
    }

    private void initComponent() {
        setContentPane(contentPane);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        sachService = ContextUtils.getBean(ISachService.class);
        lichSuNhapHangService = ContextUtils.getBean(ILichSuNhapHangService.class);

        tableModel = (DefaultTableModel) tbl.getModel();
    }

    public void fillTable() {
        tableModel.setDataVector(
                lichSuNhapHangService.getAllByIdSach(id).stream().map(i -> new Object[]{i.getNgayNhap(), i.getSoLuong(), i.getGiaNhap()}).toArray(Object[][]::new),
                new String[]{"Ngày nhập sách", "Số lượng", "Giá nhập"}
        );
    }

    public LichSuNhapHangDTO readForm() {
        LichSuNhapHangDTO lichSuNhapHangDTO = new LichSuNhapHangDTO();

        Date ngayNhap = dateNgayNhap.getDate();
        if (ngayNhap == null) {
            MsgBox.alert(null, "Vui lòng chọn ngày nhập hàng");
            return null;
        }

        try {
            int soLuong = Integer.parseInt(txtSL.getText());
            long giaNhap = Long.parseLong(txtGiaNhap.getText());
            lichSuNhapHangDTO.setGiaNhap(giaNhap);
            lichSuNhapHangDTO.setNgayNhap(ngayNhap.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            lichSuNhapHangDTO.setSoLuong(soLuong);
        } catch (NumberFormatException e) {
            MsgBox.alert(this, "Nhập số đi");
            return null;
        }
        return lichSuNhapHangDTO;
    }

    private void createUIComponents() {
        bg = new RoundPanel(10);
        btnEdit = new ButtonToolItem("pencil.png", "pencil.png");
    }
}