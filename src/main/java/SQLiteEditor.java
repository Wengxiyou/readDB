import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class SQLiteEditor extends JFrame {
    private JTabbedPane tabbedPane;
    private JTextArea sqlQueryArea;
    private JButton executeButton;
    private JButton openButton;
    private JButton newButton;
    private JButton saveButton;
    private JButton beginTransactionButton;
    private JButton commitButton;
    private JButton rollbackButton;
    private JButton exportButton;
    private JTable resultTable;
    private JComboBox<String> tablesComboBox;
    private JTextArea tableStructureArea;
    private JTextArea logArea;
    private Connection connection;
    private Statement statement;
    private DefaultTableModel tableModel;
    private File currentDatabase;
    private boolean inTransaction = false;
    private JScrollPane logScrollPane;
    
    public SQLiteEditor() {
        super("SQLite 数据库编辑器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        
        // 设置中文字体
        setUIFonts();
        
        // 初始化UI组件
        initComponents();
        
        // 添加事件监听器
        addEventListeners();
        
        // 居中显示
        setLocationRelativeTo(null);
    }
    
    private void setUIFonts() {
        // 设置全局字体，确保中文正常显示
        Font font = new Font("微软雅黑", Font.PLAIN, 12);
        UIManager.put("Label.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        
        // 查询标签页
        JPanel queryPanel = new JPanel(new BorderLayout());
        
        // 工具栏
        JToolBar toolbar = new JToolBar();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        openButton = new JButton("打开数据库");
        newButton = new JButton("新建数据库");
        saveButton = new JButton("保存");
        executeButton = new JButton("执行查询");
        beginTransactionButton = new JButton("开始事务");
        commitButton = new JButton("提交事务");
        rollbackButton = new JButton("回滚事务");
        exportButton = new JButton("导出结果");
        
        executeButton.setEnabled(false);
        saveButton.setEnabled(false);
        beginTransactionButton.setEnabled(false);
        commitButton.setEnabled(false);
        rollbackButton.setEnabled(false);
        exportButton.setEnabled(false);
        
        toolbar.add(openButton);
        toolbar.add(newButton);
        toolbar.add(saveButton);
        toolbar.addSeparator();
        toolbar.add(beginTransactionButton);
        toolbar.add(commitButton);
        toolbar.add(rollbackButton);
        toolbar.addSeparator();
        toolbar.add(executeButton);
        toolbar.add(exportButton);
        
        // SQL查询区域
        sqlQueryArea = new JTextArea(5, 80);
        sqlQueryArea.setLineWrap(false);
        sqlQueryArea.setWrapStyleWord(true);
        sqlQueryArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        sqlQueryArea.setToolTipText("输入SQL查询语句，支持Ctrl+Enter执行");
        
        // 添加快捷键支持
        InputMap inputMap = sqlQueryArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = sqlQueryArea.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "executeQuery");
        actionMap.put("executeQuery", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(sqlQueryArea);
        
        // 结果表格
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.getTableHeader().setReorderingAllowed(false); // 禁止列拖拽
        
        // 设置表格渲染器，使结果更易读
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                                                           boolean isSelected, boolean hasFocus, 
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Number) {
                    setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                return c;
            }
        });
        
        JScrollPane tableScrollPane = new JScrollPane(resultTable);
        
        // 日志区域
        logArea = new JTextArea(3, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setForeground(Color.BLUE);
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("操作日志"));
        
        // 组装查询面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(logScrollPane, BorderLayout.SOUTH);
        
        queryPanel.add(toolbar, BorderLayout.NORTH);
        queryPanel.add(centerPanel, BorderLayout.CENTER);
        queryPanel.add(tableScrollPane, BorderLayout.SOUTH);
        
        // 表结构标签页
        JPanel tablesPanel = new JPanel(new BorderLayout());
        tablesComboBox = new JComboBox<>();
        JButton refreshButton = new JButton("刷新表列表");
        JButton viewStructureButton = new JButton("查看表结构");
        JButton viewDataButton = new JButton("查看表数据");
        
        JPanel tablesToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablesToolbar.add(new JLabel("选择表: "));
        tablesToolbar.add(tablesComboBox);
        tablesToolbar.add(refreshButton);
        tablesToolbar.add(viewStructureButton);
        tablesToolbar.add(viewDataButton);
        
        tableStructureArea = new JTextArea();
        tableStructureArea.setEditable(false);
        tableStructureArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane structureScrollPane = new JScrollPane(tableStructureArea);
        
        tablesPanel.add(tablesToolbar, BorderLayout.NORTH);
        tablesPanel.add(structureScrollPane, BorderLayout.CENTER);
        
        // 添加标签页
        tabbedPane.addTab("SQL查询", queryPanel);
        tabbedPane.addTab("表结构", tablesPanel);
        
        // 添加菜单栏
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem openMenuItem = new JMenuItem("打开数据库");
        JMenuItem newMenuItem = new JMenuItem("新建数据库");
        JMenuItem saveMenuItem = new JMenuItem("保存");
        JMenuItem exportMenuItem = new JMenuItem("导出结果");
        JMenuItem exitMenuItem = new JMenuItem("退出");
        
        saveMenuItem.setEnabled(false);
        exportMenuItem.setEnabled(false);
        
        fileMenu.add(openMenuItem);
        fileMenu.add(newMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(exportMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        
        // 数据库菜单
        JMenu dbMenu = new JMenu("数据库");
        JMenuItem beginTxMenuItem = new JMenuItem("开始事务");
        JMenuItem commitTxMenuItem = new JMenuItem("提交事务");
        JMenuItem rollbackTxMenuItem = new JMenuItem("回滚事务");
        JMenuItem vacuumMenuItem = new JMenuItem("优化数据库(VACUUM)");
        
        beginTxMenuItem.setEnabled(false);
        commitTxMenuItem.setEnabled(false);
        rollbackTxMenuItem.setEnabled(false);
        
        dbMenu.add(beginTxMenuItem);
        dbMenu.add(commitTxMenuItem);
        dbMenu.add(rollbackTxMenuItem);
        dbMenu.addSeparator();
        dbMenu.add(vacuumMenuItem);
        menuBar.add(dbMenu);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutMenuItem = new JMenuItem("关于");
        JMenuItem helpMenuItem = new JMenuItem("使用帮助");
        helpMenu.add(helpMenuItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        
        // 数据库菜单事件
        beginTxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                beginTransaction();
            }
        });
        
        commitTxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitTransaction();
            }
        });
        
        rollbackTxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rollbackTransaction();
            }
        });
        
        vacuumMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDatabase();
            }
        });
        
        // 文件菜单事件
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDatabase();
            }
        });
        
        exportMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportResults();
            }
        });
        
        // 帮助菜单事件
        helpMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelpDialog();
            }
        });
        
        setJMenuBar(menuBar);
        
        // 主面板
        setContentPane(tabbedPane);
    }
    
    private void addEventListeners() {
        // 打开数据库按钮事件
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDatabase();
            }
        });
        
        // 新建数据库按钮事件
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newDatabase();
            }
        });
        
        // 保存按钮事件
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDatabase();
            }
        });
        
        // 执行查询按钮事件
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        });
        
        // 开始事务按钮事件
        beginTransactionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                beginTransaction();
            }
        });
        
        // 提交事务按钮事件
        commitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitTransaction();
            }
        });
        
        // 回滚事务按钮事件
        rollbackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rollbackTransaction();
            }
        });
        
        // 导出结果按钮事件
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportResults();
            }
        });
        
        // 获取并添加表结构标签页中的按钮事件
        JPanel tablesPanel = (JPanel)tabbedPane.getComponentAt(1);
        JPanel tablesToolbar = (JPanel)tablesPanel.getComponent(0);
        
        // 刷新表列表按钮事件
        JButton refreshButton = null;
        JButton viewStructureButton = null;
        JButton viewDataButton = null;
        
        // 遍历工具栏中的组件找到按钮
        for (Component comp : tablesToolbar.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton)comp;
                if ("刷新表列表".equals(btn.getText())) {
                    refreshButton = btn;
                } else if ("查看表结构".equals(btn.getText())) {
                    viewStructureButton = btn;
                } else if ("查看表数据".equals(btn.getText())) {
                    viewDataButton = btn;
                }
            }
        }
        
        // 添加按钮事件监听器
        if (refreshButton != null) {
            refreshButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshTableList();
                }
            });
        }
        
        if (viewStructureButton != null) {
            viewStructureButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewTableStructure();
                }
            });
        }
        
        if (viewDataButton != null) {
            viewDataButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewTableData();
                }
            });
        }
        
        // 添加菜单栏事件
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null && menuBar.getMenuCount() > 0) {
            JMenu fileMenu = menuBar.getMenu(0);
            if (fileMenu != null && fileMenu.getItemCount() > 0) {
                // 打开数据库菜单项
                JMenuItem openMenuItem = fileMenu.getItem(0);
                if (openMenuItem != null) {
                    openMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            openDatabase();
                        }
                    });
                }
                
                // 新建数据库菜单项
                JMenuItem newMenuItem = fileMenu.getItem(1);
                if (newMenuItem != null) {
                    newMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            newDatabase();
                        }
                    });
                }
                
                // 退出菜单项
                JMenuItem exitMenuItem = fileMenu.getItem(3);
                if (exitMenuItem != null) {
                    exitMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dispose();
                            System.exit(0);
                        }
                    });
                }
            }
            
            // 关于菜单项
            JMenu helpMenu = menuBar.getMenu(1);
            if (helpMenu != null && helpMenu.getItemCount() > 0) {
                JMenuItem aboutMenuItem = helpMenu.getItem(0);
                if (aboutMenuItem != null) {
                    aboutMenuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            showAboutDialog();
                        }
                    });
                }
            }
        }
    }
    
    private void openDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite数据库文件 (*.db)", "db"));
        
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            connectToDatabase(selectedFile);
        }
    }
    
    private void newDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite数据库文件 (*.db)", "db"));
        fileChooser.setDialogTitle("新建数据库");
        
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // 确保文件有.db扩展名
            if (!selectedFile.getName().endsWith(".db")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".db");
            }
            
            // 如果文件已存在，询问是否覆盖
            if (selectedFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this, "文件已存在，是否覆盖？", "确认覆盖", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            connectToDatabase(selectedFile);
        }
    }
    
    private void connectToDatabase(File file) {
        try {
            // 关闭之前的连接
            if (connection != null) {
                connection.close();
            }
            
            // 重置事务状态
            inTransaction = false;
            
            // 建立新连接
            String url = "jdbc:sqlite:" + file.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true); // 默认自动提交
            statement = connection.createStatement();
            currentDatabase = file;
            
            setTitle("SQLite 数据库编辑器 - " + file.getName());
            JOptionPane.showMessageDialog(this, "成功连接到数据库: " + file.getName());
            
            // 更新按钮状态
            updateUIState(true);
            
            // 刷新表列表
            refreshTableList();
            
            // 添加日志
            log("已连接到数据库: " + file.getName());
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "数据库连接错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("连接失败: " + ex.getMessage());
        }
    }
    
    private void updateUIState(boolean connected) {
        executeButton.setEnabled(connected);
        saveButton.setEnabled(connected);
        beginTransactionButton.setEnabled(connected && !inTransaction);
        commitButton.setEnabled(connected && inTransaction);
        rollbackButton.setEnabled(connected && inTransaction);
        exportButton.setEnabled(connected && tableModel.getRowCount() > 0);
        
        // 更新菜单项状态
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            JMenu fileMenu = menuBar.getMenu(0);
            if (fileMenu != null) {
                JMenuItem saveMenuItem = fileMenu.getItem(2);
                JMenuItem exportMenuItem = fileMenu.getItem(3);
                if (saveMenuItem != null) saveMenuItem.setEnabled(connected);
                if (exportMenuItem != null) exportMenuItem.setEnabled(connected && tableModel.getRowCount() > 0);
            }
            
            JMenu dbMenu = menuBar.getMenu(1);
            if (dbMenu != null) {
                JMenuItem beginTxMenuItem = dbMenu.getItem(0);
                JMenuItem commitTxMenuItem = dbMenu.getItem(1);
                JMenuItem rollbackTxMenuItem = dbMenu.getItem(2);
                if (beginTxMenuItem != null) beginTxMenuItem.setEnabled(connected && !inTransaction);
                if (commitTxMenuItem != null) commitTxMenuItem.setEnabled(connected && inTransaction);
                if (rollbackTxMenuItem != null) rollbackTxMenuItem.setEnabled(connected && inTransaction);
            }
        }
    }
    
    private void log(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void beginTransaction() {
        try {
            if (connection != null) {
                connection.setAutoCommit(false);
                inTransaction = true;
                updateUIState(true);
                log("事务已开始");
                JOptionPane.showMessageDialog(this, "事务已开始，请执行您的SQL操作，完成后提交或回滚事务。");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "开始事务失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("开始事务失败: " + ex.getMessage());
        }
    }
    
    private void commitTransaction() {
        try {
            if (connection != null && inTransaction) {
                connection.commit();
                connection.setAutoCommit(true);
                inTransaction = false;
                updateUIState(true);
                log("事务已提交");
                JOptionPane.showMessageDialog(this, "事务已成功提交");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "提交事务失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("提交事务失败: " + ex.getMessage());
        }
    }
    
    private void rollbackTransaction() {
        try {
            if (connection != null && inTransaction) {
                connection.rollback();
                connection.setAutoCommit(true);
                inTransaction = false;
                updateUIState(true);
                log("事务已回滚");
                JOptionPane.showMessageDialog(this, "事务已成功回滚");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "回滚事务失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("回滚事务失败: " + ex.getMessage());
        }
    }
    
    private void exportResults() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "没有可导出的数据", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出结果");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV文件 (*.csv)", "csv"));
        
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // 写入列名
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (i > 0) writer.write(",");
                    writer.write("\"" + tableModel.getColumnName(i) + "\"");
                }
                writer.newLine();
                
                // 写入数据
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        if (j > 0) writer.write(",");
                        Object value = tableModel.getValueAt(i, j);
                        String text = value != null ? value.toString() : "";
                        writer.write("\"" + text.replace("\"", "\"\"") + "\"");
                    }
                    writer.newLine();
                }
                
                log("查询结果已导出到: " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "数据已成功导出到: " + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                log("导出失败: " + ex.getMessage());
            }
        }
    }
    
    private void showHelpDialog() {
        JDialog helpDialog = new JDialog(this, "使用帮助", true);
        helpDialog.setSize(600, 500);
        
        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        
        String helpContent = "SQLite数据库编辑器使用帮助\n\n"
                + "1. 基本操作:\n"
                + "   - 打开数据库: 点击\"打开数据库\"按钮或选择文件菜单中的对应选项\n"
                + "   - 新建数据库: 点击\"新建数据库\"按钮或选择文件菜单中的对应选项\n"
                + "   - 执行SQL查询: 在SQL查询区域输入语句，点击\"执行查询\"按钮或按Ctrl+Enter\n"
                + "   - 保存数据库: 点击\"保存\"按钮，执行VACUUM命令优化数据库\n\n"
                + "2. 事务操作:\n"
                + "   - 开始事务: 点击\"开始事务\"按钮\n"
                + "   - 提交事务: 执行完SQL操作后，点击\"提交事务\"按钮\n"
                + "   - 回滚事务: 如果不想保存更改，点击\"回滚事务\"按钮\n\n"
                + "3. 表操作:\n"
                + "   - 查看表结构: 在\"表结构\"标签页选择表，点击\"查看表结构\"\n"
                + "   - 查看表数据: 在\"表结构\"标签页选择表，点击\"查看表数据\"\n"
                + "   - 刷新表列表: 点击\"刷新表列表\"按钮\n\n"
                + "4. 数据导出:\n"
                + "   - 将查询结果导出为CSV文件: 执行查询后点击\"导出结果\"按钮\n\n"
                + "5. 快捷键:\n"
                + "   - Ctrl+Enter: 执行SQL查询\n\n"
                + "6. 注意事项:\n"
                + "   - 请确保已正确安装SQLite JDBC驱动\n"
                + "   - 大型操作建议使用事务功能\n"
                + "   - 定期使用VACUUM命令优化数据库性能";
        
        helpText.setText(helpContent);
        helpDialog.add(new JScrollPane(helpText));
        helpDialog.setLocationRelativeTo(this);
        helpDialog.setVisible(true);
    }
    
    private void executeQuery() {
        String sql = sqlQueryArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL查询语句", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        log("执行SQL: " + sql);
        
        try {
            // 清空表格
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            
            boolean hasResultSet = statement.execute(sql);
            
            if (hasResultSet) {
                // 处理SELECT查询结果
                ResultSet resultSet = statement.getResultSet();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // 设置列名
                Vector<String> columnNames = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }
                tableModel.setColumnIdentifiers(columnNames);
                
                // 添加数据行
                int rowCount = 0;
                while (resultSet.next()) {
                    Vector<Object> rowData = new Vector<>();
                    for (int i = 1; i <= columnCount; i++) {
                        rowData.add(resultSet.getObject(i));
                    }
                    tableModel.addRow(rowData);
                    rowCount++;
                }
                
                resultSet.close();
                
                // 调整列宽
                autoResizeColumns();
                
                log("查询完成，返回了 " + rowCount + " 行数据");
            } else {
                // 处理UPDATE/INSERT/DELETE等语句
                int rowsAffected = statement.getUpdateCount();
                String message = "操作成功，影响了 " + rowsAffected + " 行数据";
                if (inTransaction) {
                    message += "（在事务中，尚未提交）";
                    log(message);
                } else {
                    JOptionPane.showMessageDialog(this, message);
                    log(message);
                }
                
                // 如果是创建或修改表的操作，刷新表列表
                String upperSQL = sql.toUpperCase();
                if (upperSQL.contains("CREATE TABLE") || upperSQL.contains("DROP TABLE") || upperSQL.contains("ALTER TABLE")) {
                    refreshTableList();
                }
            }
            
            // 更新导出按钮状态
            updateUIState(true);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "SQL执行错误: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("SQL执行错误: " + ex.getMessage());
        }
    }
    
    private void autoResizeColumns() {
        // 调整列宽以适应内容
        for (int column = 0; column < resultTable.getColumnCount(); column++) {
            TableColumn tableColumn = resultTable.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getMinWidth();
            int maxWidth = tableColumn.getMaxWidth();
            
            // 计算每列的最佳宽度
            for (int row = 0; row < resultTable.getRowCount(); row++) {
                TableCellRenderer cellRenderer = resultTable.getCellRenderer(row, column);
                Component c = resultTable.prepareRenderer(cellRenderer, row, column);
                int width = c.getPreferredSize().width + resultTable.getIntercellSpacing().width;
                preferredWidth = Math.max(preferredWidth, width);
                
                // 限制最大宽度，避免过宽的列
                if (preferredWidth >= maxWidth) {
                    preferredWidth = maxWidth;
                    break;
                }
            }
            
            // 设置列宽，但不超过最大限制
            preferredWidth = Math.min(preferredWidth, 500); // 最大宽度限制为500像素
            tableColumn.setPreferredWidth(preferredWidth);
        }
    }
    
    private void refreshTableList() {
        try {
            if (connection == null) return;
            
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            tablesComboBox.removeAllItems();
            while (resultSet.next()) {
                tablesComboBox.addItem(resultSet.getString("TABLE_NAME"));
            }
            
            resultSet.close();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "获取表列表失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewTableStructure() {
        String tableName = (String) tablesComboBox.getSelectedItem();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个表", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            StringBuilder structure = new StringBuilder();
            structure.append("表名: ").append(tableName).append("\n\n");
            structure.append("列信息:\n");
            structure.append(String.format("%-20s %-20s %-10s %-10s\n", "列名", "数据类型", "是否为空", "默认值"));
            structure.append("----------------------------------------------------------------------\n");
            
            // 获取表结构
            ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")");
            while (resultSet.next()) {
                String columnName = resultSet.getString("name");
                String type = resultSet.getString("type");
                String notNull = resultSet.getInt("notnull") == 1 ? "NOT NULL" : "NULL";
                String defaultValue = resultSet.getString("dflt_value");
                if (defaultValue == null) defaultValue = "";
                
                structure.append(String.format("%-20s %-20s %-10s %-10s\n", 
                        columnName, type, notNull, defaultValue));
            }
            resultSet.close();
            
            // 获取索引信息
            structure.append("\n索引信息:\n");
            resultSet = statement.executeQuery("PRAGMA index_list(" + tableName + ")");
            if (!resultSet.next()) {
                structure.append("无索引\n");
            } else {
                do {
                    String indexName = resultSet.getString("name");
                    String unique = resultSet.getInt("unique") == 1 ? "唯一" : "非唯一";
                    structure.append("索引名: " + indexName + " (" + unique + ")\n");
                    
                    // 获取索引列信息
                    ResultSet indexColumns = statement.executeQuery("PRAGMA index_info(" + indexName + ")");
                    structure.append("  索引列: ");
                    boolean firstColumn = true;
                    while (indexColumns.next()) {
                        if (!firstColumn) structure.append(", ");
                        structure.append(indexColumns.getString("name"));
                        firstColumn = false;
                    }
                    structure.append("\n");
                    indexColumns.close();
                } while (resultSet.next());
            }
            resultSet.close();
            
            // 显示表结构
            tableStructureArea.setText(structure.toString());
            tableStructureArea.setCaretPosition(0);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "获取表结构失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewTableData() {
        String tableName = (String) tablesComboBox.getSelectedItem();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个表", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            // 切换到SQL查询标签页
            tabbedPane.setSelectedIndex(0);
            
            // 设置SQL语句
            sqlQueryArea.setText("SELECT * FROM " + tableName + ";");
            
            // 执行查询
            executeQuery();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "查看表数据失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveDatabase() {
        // SQLite数据库是自动保存的，这里可以添加一些优化或压缩操作
        try {
            // 执行VACUUM命令优化数据库
            statement.execute("VACUUM;");
            JOptionPane.showMessageDialog(this, "数据库保存成功");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "数据库保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "SQLite数据库编辑器 v1.0\n" +
                "一个简单易用的SQLite数据库编辑工具\n" +
                "支持SQL查询、表结构查看等功能\n\n" +
                "© 2023 SQLite Editor",
                "关于", JOptionPane.INFORMATION_MESSAGE);
    }
    
    @Override
    public void dispose() {
        // 关闭数据库连接
        try {
            if (connection != null && !connection.isClosed()) {
                // 如果在事务中，尝试回滚
                if (inTransaction) {
                    try {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        log("应用程序关闭，未提交的事务已回滚");
                    } catch (SQLException ex) {
                        log("回滚事务失败: " + ex.getMessage());
                    }
                }
                connection.close();
                log("数据库连接已关闭");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        // 尝试加载SQLite驱动（兼容不同的类加载环境）
        boolean driverLoaded = false;
        try {
            // 方法1：显式加载
            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;
        } catch (ClassNotFoundException e1) {
            try {
                // 方法2：尝试通过DriverManager自动加载
                // 这种方式在依赖被打包到jar中时可能更有效
                String url = "jdbc:sqlite:test.db";
                DriverManager.getConnection(url).close();
                driverLoaded = true;
            } catch (SQLException e2) {
                // 驱动确实未找到
                System.err.println("未找到SQLite JDBC驱动，请添加sqlite-jdbc.jar到类路径");
                JOptionPane.showMessageDialog(null, 
                        "未找到SQLite JDBC驱动，请添加sqlite-jdbc.jar到类路径", 
                        "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        
        // 在事件调度线程中运行GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SQLiteEditor().setVisible(true);
            }
        });
    }
}