package com.sax.views.quanly.views.panes;

import com.sax.services.IAccountService;
import com.sax.services.impl.AccountService;
import com.sax.utils.ContextUtils;
import com.sax.utils.MsgBox;
import com.sax.utils.Session;
import com.sax.views.components.ListPageNumber;
import com.sax.views.components.Loading;
import com.sax.views.components.Search;
import com.sax.views.components.libraries.ButtonToolItem;
import com.sax.views.components.libraries.RoundPanel;
import com.sax.views.quanly.viewmodel.AbstractViewObject;
import com.sax.views.quanly.viewmodel.NhanVienViewObject;
import com.sax.views.quanly.views.dialogs.NhanVienDialog;
import com.sax.views.quanly.views.dialogs.TaiKhoanDialog;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXTable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class NhanVienPane extends JPanel {
    private JXTable table;
    private JPanel bg;
    private JPanel nhanVienPanel;
    private JButton btnAdd;
    private JButton btnDel;
    private JButton btnEdit;
    private JCheckBox cbkSelectedAll;
    private JPanel phanTrangPane;
    private JComboBox cboHienThi;
    private JList listPage;
    private Search timKiem;
    private JButton btnDoiMatKhau;
    private IAccountService accountService = ContextUtils.getBean(AccountService.class);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<JCheckBox> listCbk = new ArrayList<>();
    private Set tempIdSet = new HashSet();
    private Loading loading = new Loading(this);

    private DefaultListModel listPageModel = new DefaultListModel();
    @Getter
    @Setter
    private int sizeValue = 14;
    @Getter
    @Setter
    private int pageValue = 1;
    @Getter
    @Setter
    private Pageable pageable = PageRequest.of(pageValue - 1, sizeValue);
    private Timer timer;

    public NhanVienPane() {
        initComponent();
        btnAdd.addActionListener((e) -> add());
        btnEdit.addActionListener((e) -> update());
        btnDel.addActionListener((e) -> delete());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) update();
            }
        });
        listPage.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectPageDisplay();
            }
        });
        cboHienThi.addActionListener((e) -> selectSizeDisplay());
        timKiem.txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                timer.restart();
            }
        });
        btnDoiMatKhau.addActionListener((e) -> openDoiMatKhau());
        cbkSelectedAll.addActionListener((e) -> Session.chonTatCa(cbkSelectedAll, table, listCbk, tempIdSet));
    }

    public void initComponent() {
        ((DefaultTableModel) table.getModel()).setColumnIdentifiers(new String[]{"", "MNV", "Tên nhân viên", "Username", "Email", "Số điện thoại", "Giới tính", "Vai trò", "Ngày thêm", "Trạng thái"});
        new Worker().execute();
        loading.setVisible(true);
        timer = new Timer(300, e -> {
            searchByKeyword();
            timer.stop();
        });
    }

    public void fillTable(List<AbstractViewObject> list) {
        Session.fillTable(list, table, cbkSelectedAll, executorService, tempIdSet, listCbk);
    }

    private void add() {
        TaiKhoanDialog nhanVienDialog = new TaiKhoanDialog();
        nhanVienDialog.parentPane = this;
        nhanVienDialog.lblTitle.setText("Thêm mới tài khoản nhân viên");
        nhanVienDialog.setVisible(true);
        table.clearSelection();
    }

    private void openDoiMatKhau() {
        if (table.getSelectedRow() >= 0) {
            TaiKhoanDialog taiKhoanDialog = new TaiKhoanDialog();
            taiKhoanDialog.parentPane = this;
            taiKhoanDialog.id = (int) table.getValueAt(table.getSelectedRow(), 1);
            taiKhoanDialog.lblTitle.setText("Đổi mật khẩu tài khoản nhân viên");
            taiKhoanDialog.fillForm();
            taiKhoanDialog.setVisible(true);
            table.clearSelection();
        }
    }

    private void update() {
        if (table.getSelectedRow() >= 0) {
            executorService.submit(() -> {
                NhanVienDialog nhanVienDialog = new NhanVienDialog();
                nhanVienDialog.parentPane = this;
                nhanVienDialog.id = (int) table.getValueAt(table.getSelectedRow(), 1);
                nhanVienDialog.fillForm();
                loading.dispose();
                nhanVienDialog.setVisible(true);
                table.clearSelection();
            });
            loading.setVisible(true);
        } else MsgBox.alert(this, "Vui lòng chọn một tài khoản!");
    }

    private void delete() {
        if (!tempIdSet.isEmpty()) {
            boolean check = MsgBox.confirm(null, "Bạn có muốn xoá " + tempIdSet.size() + " tài khoản này không?");
            if (check) {
                try {
                    accountService.deleteAll(tempIdSet);
                    cbkSelectedAll.setSelected(false);
                } catch (Exception e) {
                    MsgBox.alert(this, e.getMessage());
                }
                pageValue = accountService.getTotalPage(sizeValue) < pageValue ? accountService.getTotalPage(sizeValue) : pageValue;
                pageable = PageRequest.of(pageValue - 1, sizeValue);
                fillTable(accountService.getPage(pageable).stream().map(NhanVienViewObject::new).collect(Collectors.toList()));
                fillListPage();
                loading.dispose();
            }
        } else MsgBox.alert(this, "Vui lòng tick vào ít nhất một tài khoản!");
    }

    public void searchByKeyword() {
        String keyword = timKiem.txtSearch.getText();
        if (!keyword.isEmpty()) {
            fillTable(accountService.searchByKeyword(keyword).stream().map(NhanVienViewObject::new).collect(Collectors.toList()));
            phanTrangPane.setVisible(false);
        } else {
            fillTable(accountService.getAll().stream().map(NhanVienViewObject::new).collect(Collectors.toList()));
            phanTrangPane.setVisible(true);
        }
    }

    public void fillListPage() {
        Session.fillListPage(pageValue, listPageModel, accountService, sizeValue, listPage);
    }

    public void selectPageDisplay() {
        if (listPage.getSelectedValue() instanceof Integer) {
            pageValue = Integer.parseInt(listPage.getSelectedValue().toString());
            pageable = PageRequest.of(pageValue - 1, sizeValue);
            new Worker().execute();
            loading.setVisible(true);
        }
    }

    public void selectSizeDisplay() {
        sizeValue = Integer.parseInt(cboHienThi.getSelectedItem().toString());
        pageValue = 1;
        pageable = PageRequest.of(pageValue - 1, sizeValue);
        new Worker().execute();
        loading.setVisible(true);
    }


    private void createUIComponents() {
        nhanVienPanel = this;
        bg = new RoundPanel(10);
        btnAdd = new ButtonToolItem("add.svg", "add.svg");
        btnDel = new ButtonToolItem("trash-c.svg", "trash-c.svg");
        btnEdit = new ButtonToolItem("pencil.svg", "pencil.svg");
        btnDoiMatKhau = new ButtonToolItem("pencil.svg", "pencil.svg");

        listPage = new ListPageNumber();
    }

    class Worker extends SwingWorker<List<AbstractViewObject>, Integer> {
        @Override
        protected List<AbstractViewObject> doInBackground() {
            return accountService.getPage(pageable).stream().map(NhanVienViewObject::new).collect(Collectors.toList());
        }

        @Override
        protected void done() {
            try {
                fillTable(get());
                if (table.getRowCount() > 0) fillListPage();
                loading.dispose();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
