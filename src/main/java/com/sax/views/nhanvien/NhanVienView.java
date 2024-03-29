package com.sax.views.nhanvien;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.sax.dtos.*;
import com.sax.services.*;
import com.sax.services.impl.AccountService;
import com.sax.services.impl.DanhMucService;
import com.sax.services.impl.DonHangService;
import com.sax.services.impl.KhachHangService;
import com.sax.utils.*;
import com.sax.views.components.ComboBoxSearch;
import com.sax.views.components.Loading;
import com.sax.views.components.Search;
import com.sax.views.components.libraries.ButtonToolItem;
import com.sax.views.components.libraries.CustomScrollPane;
import com.sax.views.components.libraries.RoundPanel;
import com.sax.views.components.libraries.WrapLayout;
import com.sax.views.nhanvien.cart.CustomCart;
import com.sax.views.nhanvien.dialog.*;
import com.sax.views.nhanvien.dialog.hoadon.HoaDonDialog;
import com.sax.views.nhanvien.doncho.DonChoViewObject;
import com.sax.views.nhanvien.product.ProductItem;
import com.sax.views.quanly.views.dialogs.CameraDialog;
import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXTable;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class NhanVienView extends JPanel {
    public static NhanVienView nvv;
    private JPanel contentPane;
    private JPanel categoryPane;
    private JPanel cartPane;
    private JScrollPane scrollDon;
    private JPanel donItem;
    private JScrollPane scrollBo;
    private JPanel boItem;
    private JButton btnDon;
    private JButton btnBo;
    private JPanel tabContent;
    private JButton btnScan;
    private JComboBox cboKH;
    private JXTable cart;
    private JRadioButton rdoTM;
    private JRadioButton rdoNH;
    private JButton btnThanhToan;
    private JButton xButton;
    private JLabel lblTPT;
    private JLabel lblTienHang;
    private JLabel lblChietKhau;
    private JList danhMuc;
    private JButton btnDonHang;
    private JButton btnTK;
    private JLabel lblLogo;
    private JPanel avatar;
    private JLabel lblNV;
    private Search timKiem;
    private JCheckBox chkDiem;
    private JButton btnKhachHang;
    private JPanel useDiem;
    private JButton btnDonHangCho;
    private JButton btnThemTam;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ISachService sachService = ContextUtils.getBean(ISachService.class);
    private IDonHangService donHangService = ContextUtils.getBean(DonHangService.class);
    private IKhachHangService khachHangService = ContextUtils.getBean(KhachHangService.class);
    private IAccountService accountService = ContextUtils.getBean(AccountService.class);
    private IDanhMucService danhMucService = ContextUtils.getBean(DanhMucService.class);
    private DefaultListModel danhMucLM = new DefaultListModel();
    private Loading loading = new Loading(this);
    private Timer timer;

    public NhanVienView() {
        intiComponent();
        btnDon.addActionListener((e) -> {
            ((CardLayout) tabContent.getLayout()).show(tabContent, "don");
            fillSach(sachService.getAllSachInOrNotInCTKM());
        });
        btnBo.addActionListener((e) -> {
            ((CardLayout) tabContent.getLayout()).show(tabContent, "bo");
        });
        btnThanhToan.addActionListener((e) -> thanhToan());
        xButton.addActionListener((e) -> clear());
        btnKhachHang.addActionListener((e) -> openKhachHang());
        btnDonHang.addActionListener((e) -> openDonHang());
        danhMuc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chonDanhMuc();
            }
        });
        ((JLayeredPane) Session.avatar.getComponent(0)).getComponent(0).addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUserPopup();
            }
        });
        timKiem.txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                timer.restart();
            }
        });
        btnScan.addActionListener((e) -> openScan());
        cboKH.addActionListener(e -> fillDiem());
        chkDiem.addActionListener(e -> Cart.tinhTien(cart, lblTienHang, lblChietKhau, lblTPT, chkDiem));
        btnThemTam.addActionListener((e) -> themTam());
        btnDonHangCho.addActionListener((e) -> openDonHangCho());
    }

    private void intiComponent() {
        nvv = this;
        donItem.setLayout(new WrapLayout(WrapLayout.LEFT, 10, 10));
        new Worker(0).execute();
        loading.setVisible(true);
        fillDanhMuc();
        danhMuc.setCursor(new Cursor(Cursor.HAND_CURSOR));
        danhMuc.setSelectedIndex(0);
        fillKhachHang(khachHangService.getAll());
        ((CustomCart) cart).initComponent();
        Session.lblName = lblNV;
        Session.avatar = avatar;
        lblNV.setText(Session.accountid.getTenNhanVien());
        avatar.add(ImageUtils.getCircleImage(Session.accountid.getAnh(), 30, 30, null, 0));
        lblLogo.setIcon(new ImageIcon(ImageUtils.readImage("logo.png").getScaledInstance(73, 50, Image.SCALE_SMOOTH)));
        ((JLayeredPane) avatar.getComponent(0)).getComponent(0).setCursor(new Cursor(Cursor.HAND_CURSOR));
        timer = new Timer(300, e -> {
            searchByKeyword();
            timer.stop();
        });
    }

    private void setSelected(ProductItem com) {
        Arrays.stream(donItem.getComponents()).forEach(i -> {
            ProductItem pdi = (ProductItem) i;
            if (pdi.isSelected()) pdi.setSelected(false);
        });
        com.setSelected(true);
    }

    private void fillDanhMuc() {
        danhMuc.setModel(danhMucLM);
        danhMucLM.addElement("Tất cả");
        danhMucService.getAll().forEach(i -> danhMucLM.addElement(i));
        danhMuc.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JButton btn = new JButton(value.toString().replace("-", "  "));
                btn.setBorderPainted(false);
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                btn.setFont(new Font(".SF NS Text", 4, 13));
                if (!value.toString().contains("-")) btn.setFont(new Font(".SF NS Text", 1, 13));

                if (isSelected) {
                    btn.setBackground(Color.decode("#EA6C20"));
                    btn.setForeground(Color.WHITE);
                }
                return btn;
            }
        });
    }

    private void fillSach(List<SachDTO> list) {
        donItem.removeAll();
        list.forEach(i -> {
            ProductItem pdi = new ProductItem(cart, lblTienHang, lblChietKhau, lblTPT, chkDiem);
            pdi.setData(i);
            donItem.add(pdi);
            pdi.revalidate();
            donItem.revalidate();
            pdi.getBtnAddToCart().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelected(pdi);
                }
            });
        });
        donItem.revalidate();
        donItem.repaint();
    }

    private void thanhToan() {
        try {
            DonHangDTO donHangDTO = readCart();
            if (donHangDTO.getChiTietDonHangs().size() > 0) {
                donHangDTO.setId(donHangService.insert(donHangDTO).getId());
                new HoaDonDialog(this, donHangDTO, true).setVisible(true);
                new Worker(0).execute();
                fillKhachHang(khachHangService.getAll());
                loading.setVisible(true);
                clear();
            }
        } catch (SQLServerException | FileNotFoundException | InvalidDataAccessApiUsageException e) {
            MsgBox.alert(this, e.getMessage());
        }
    }

    private void clear() {
        chkDiem.setSelected(false);
        useDiem.setVisible(false);
        Cart.getCart().clear();
        Cart.tinhTien(cart, lblTienHang, lblChietKhau, lblTPT, chkDiem);
    }

    public void fillKhachHang(List<KhachHangDTO> list) {
        cboKH.removeAllItems();
        list.forEach(i -> cboKH.addItem(i));
    }

    private DonHangDTO readCart() {
        KhachHangDTO kh = (KhachHangDTO) cboKH.getSelectedItem();
        AccountDTO nv = Session.accountid;
        long tienHang = CurrencyConverter.parseLong(lblTienHang.getText());
        long chietKhau = CurrencyConverter.parseLong(lblChietKhau.getText().replace("-", ""));
        long tienPhaiTra = Long.valueOf(lblTPT.getText().substring(0, lblTPT.getText().length() - 1).replace(".", ""));
        boolean pttt = rdoTM.isSelected() ? true : false;

        List<ChiTietDonHangDTO> chiTietDonHangDTOList = Cart.getCart().stream().map(cm -> {
            long giaGiam = cm.getGiaBan() - cm.getDonGia();
            int soLuong = (int) cm.getSoLuong().getValue();
            return new ChiTietDonHangDTO(sachService.getById(cm.getId()), giaGiam, cm.getGiaBan(), soLuong, "");
        }).collect(Collectors.toList());
        return new DonHangDTO(kh, nv, tienPhaiTra, LocalDateTime.now(), pttt, chietKhau, tienHang, chiTietDonHangDTOList);
    }

    private void openKhachHang() {
        new KhachHangNVDialog().setVisible(true);
    }

    private void openDonHang() {
        new DonHangDialog().setVisible(true);
    }

    private void chonDanhMuc() {
        if (danhMuc.getSelectedIndex() >= 0) {
            timKiem.txtSearch.setText("");
            if (danhMuc.getSelectedValue() instanceof String) {
                new Worker(0).execute();
                loading.setVisible(true);
            } else {
                new Worker(1).execute();
                loading.setVisible(true);
            }
        }
    }

    private void openUserPopup() {
        UserPopup userPopup = new UserPopup();
        userPopup.setVisible(true);
    }

    public void searchByKeyword() {
        String keyword = timKiem.txtSearch.getText();
        if (!keyword.isEmpty()) {
            fillSach(sachService.getAllAvailableSachByKeyWord(keyword));
        } else {
            new Worker(0).execute();
            loading.setVisible(true);
        }
    }

    private void fillDiem() {
        if (cboKH.getSelectedItem() != null) {
            int diem = ((KhachHangDTO) cboKH.getSelectedItem()).getDiem();
            if (diem >= 0) {
                useDiem.setVisible(true);
                chkDiem.setText(String.valueOf(diem));
            } else {
                chkDiem.setText("0");
                useDiem.setVisible(false);
                chkDiem.setSelected(false);
            }
            Cart.tinhTien(cart, lblTienHang, lblChietKhau, lblTPT, chkDiem);
        }
    }

    private void openScan() {
        CameraDialog dialog = new CameraDialog(cart, lblTienHang, lblChietKhau, lblTPT, chkDiem);
        dialog.setVisible(true);
    }

    private void themTam() {
        if (Cart.getCart().size() > 0) {
            DonChoViewObject donCho = new DonChoViewObject();
            donCho.setId(Session.listDonCho.size() + 1);
            donCho.setTienHang(lblTienHang.getText());
            donCho.setChietKhau(lblChietKhau.getText());
            donCho.setTongtien(lblTPT.getText());
            donCho.setListCart(new ArrayList<>(Cart.getCart()));
            if (cboKH.getSelectedIndex() == 0) {
                KhachHangDTO khachHangDTO = new KhachHangDTO();
                String tenKhachHang = MsgBox.prompt(this, "Nhập tên khách hàng");
                if (tenKhachHang == null) return;
                khachHangDTO.setTenKhach(tenKhachHang);
                donCho.setKhachHang(khachHangDTO);
            } else donCho.setKhachHang((KhachHangDTO) cboKH.getSelectedItem());
            Session.listDonCho.add(donCho);
            clear();
        }
    }

    private void openDonHangCho() {
        new DonHangChoDialog().setVisible(true);
    }

    private void createUIComponents() {
        contentPane = this;
        categoryPane = new RoundPanel(10);
        cartPane = new RoundPanel(10);
        tabContent = new RoundPanel(10);

        scrollDon = new CustomScrollPane();
        scrollBo = new CustomScrollPane();

        donItem = new RoundPanel(10);
        boItem = new RoundPanel(10);

        btnScan = new ButtonToolItem("barcode.svg", "barcode.svg");
        btnDon = new ButtonToolItem("ctkm.svg", "ctkm.svg");
        btnBo = new ButtonToolItem("ctkm-c.svg", "ctkm-c.svg");
        btnThanhToan = new ButtonToolItem("ctkm.svg", "ctkm.svg");
        xButton = new ButtonToolItem("x-c.svg", "x-c.svg");
        btnKhachHang = new ButtonToolItem("khachhang-c.svg", "khachhang-c.svg");
        btnDonHang = new ButtonToolItem("donhang-c.svg", "donhang-c.svg");
        btnTK = new ButtonToolItem("tknv-c.svg", "tknv-c.svg");
        btnThemTam = new ButtonToolItem("", "");

        cboKH = new ComboBoxSearch();

        cart = new CustomCart(Cart.getCart());
    }

    class Worker extends SwingWorker<List<SachDTO>, Integer> {
        int type;

        public Worker(int type) {
            this.type = type;
        }

        @Override
        protected List<SachDTO> doInBackground() {
            switch (type) {
                case 0 -> {
                    return sachService.getAllSachInOrNotInCTKM();
                }

                default -> {
                    DanhMucDTO danhMucDTO = (DanhMucDTO) danhMuc.getSelectedValue();
                    return sachService.getAllSachByIdDanhMuc(danhMucDTO.getId());
                }
            }

        }

        @Override
        protected void done() {
            try {
                fillSach(get());
                loading.dispose();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
