package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class AccountDAO {
	public AccountDAO() throws ClassNotFoundException {
		Class.forName(DbInfo.DRIVER);
	}

	public void closeAll(PreparedStatement pstmt, Connection con) throws SQLException {
		if (pstmt != null)
			pstmt.close();
		if (con != null)
			con.close();
	}

	public void closeAll(ResultSet rs, PreparedStatement pstmt, Connection con) throws SQLException {
		if (rs != null)
			rs.close();
		closeAll(pstmt, con);
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(DbInfo.URL, DbInfo.USERNAME, DbInfo.PASSWORD);
	}

	public void createAccount(AccountVO accountVO) throws CreateAccountException, SQLException {
		if (accountVO.getBalance() < 1000)
			throw new CreateAccountException("계좌 개설시 초기 납입금은 1000원 이상이어야 합니다");
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = getConnection();
			StringBuilder sql = new StringBuilder("insert into account(account_no,name,password,balance) ");
			sql.append("values(account_seq.nextval,?,?,?)");
			pstmt = con.prepareStatement(sql.toString());
			pstmt.setString(1, accountVO.getName());
			pstmt.setString(2, accountVO.getPassword());
			pstmt.setInt(3, accountVO.getBalance());
			pstmt.executeUpdate();
		} finally {
			closeAll(pstmt, con);
		}
	}

	public int findBalanceByAccountNo(String accountNo, String password)
			throws SQLException, AccountNotFoundException, NotMatchedPasswordException {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int balance = 0;
		try {
			con = getConnection();
			String sql = "SELECT password, balance FROM account where account_no=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, accountNo);
			rs = pstmt.executeQuery();
			if (rs.next()) { // 계좌번호에 해당하는 계좌가 있으면
				if (rs.getString(1).equals(password)) // 매개변수 password와 db의 password가 일치하면
					balance = rs.getInt(2);
				else // 매개변수 password와 db의 password가 일치하지 않으면
					throw new NotMatchedPasswordException("계좌의 비밀번호가 일치하지 않습니다");
			} else { // 계좌번호에 해당하는 계좌가 존재하지 않으면
				throw new AccountNotFoundException("계좌번호에 해당하는 계좌가 존재하지 않습니다");
			}
			pstmt.close();
			rs.close();
		} finally {
			closeAll(rs, pstmt, con);
		}
		return balance;
	}

	/**
	 * 계좌번호 존재 유무와 비밀번호 일치 여부를 확인하는 메서드 매개변수에 전달된 계좌번호가 존재하지 않으면
	 * AccountNotFoundException 발생시키고 전파 매개변수에 전달된 계좌번호에 대한 비밀번호가 일치하지 않으면
	 * NotMatchedPasswordException 발생시키고 전파
	 * 
	 * @param accountNo
	 * @param password
	 * @throws SQLException
	 * @throws AccountNotFoundException
	 * @throws NotMatchedPasswordException
	 */
	public void checkAccountNoAndPassword(String accountNo, String password)
			throws SQLException, AccountNotFoundException, NotMatchedPasswordException {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			String sql = "SELECT password FROM account WHERE account_no=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, accountNo);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				if (!(rs.getString(1).equals(password)))
					throw new NotMatchedPasswordException("비밀번호가 일치하지 않습니다");
			} else {
				throw new AccountNotFoundException(accountNo + "계좌번호에 해당하는 계좌가 존재하지 않습니다");
			}

		} finally {
			closeAll(rs, pstmt, con);
		}

	}

	/**
	 * 계좌에 입금하는 메서드 입금액이 0원 이하이면 noMoneyException을 발생시키고 전파 계좌번호가 존재하지 않으면
	 * AccountNotFoundException을 발생시키고 전파 패스원드가 일치하지 않으면
	 * NotMatchedPasswordException을 발생시키고 전파 입금액이 0원을 초과하고 계좌번호 존재하고 패스워드도 일치하면 입금처리
	 * 
	 * @param accountNo
	 * @param password
	 * @param money
	 * @throws NoMoneyException
	 * @throws NotMatchedPasswordException
	 * @throws AccountNotFoundException
	 * @throws SQLException
	 */
	public void deposit(String accountNo, String password, int money)
			throws NoMoneyException, SQLException, AccountNotFoundException, NotMatchedPasswordException {
		if (money <= 0)
			throw new NoMoneyException("입금액을 0원이상 설정해주세요");
		// 아래 메서들 계좌번호 존재유무와 비밀번호 일치여부를 체크한다.
		checkAccountNoAndPassword(accountNo, password);

		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = getConnection();
			String sql = "UPDATE account SET balance =balance+? WHERE account_no=? AND password=? ";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, money);
			pstmt.setString(2, accountNo);
			pstmt.setString(3, password);
			pstmt.executeUpdate();
		} finally {
			closeAll(pstmt, con);
		}
	}

	public void withdraw(String accountNo, String password, int money) throws SQLException, AccountNotFoundException,
			NotMatchedPasswordException, NoMoneyException, InsufficientBalanceException {
		if (money <= 0)
			throw new NoMoneyException("출금액을 0원이상 설정해주세요");

		int balance = findBalanceByAccountNo(accountNo, password);
		if (balance < money) {
			throw new InsufficientBalanceException("잔액이 부족합니다");
		}
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = getConnection();
			String sql = "UPDATE account SET balance=balance-? WHERE account_no=? ";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, money);
			pstmt.setString(2, accountNo);
			pstmt.executeUpdate();
		} finally {
			closeAll(pstmt, con);
		}
	}

	public boolean existAccountNo(String accountNo) throws SQLException {
		boolean result = false;
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			String sql = "SELECT COUNT(*) FROM account WHERE account_no=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, accountNo);
			rs = pstmt.executeQuery();
			if (rs.next() && rs.getInt(1) == 1) // 조회결과가 1이면 존재하므로 true를 할당
				result = true;
		} finally {
			closeAll(rs, pstmt, con);
		}
		return result;
	}

	/**
	 * 계좌이체 메서드
	 * 
	 * 이체액이 0원 초과인지를 확인 수금자 계좌번호 확인 송금자 계좌번호와 비밀번호 확인 송금자 계좌잔액을 확인(이체액보다 잔액이 적으면
	 * 예외발생, 전파)
	 * 
	 * con.setAutoCommit(false)
	 * 
	 * 송금자 계좌 출금작업 수금자 계좌 입금작업
	 * 
	 * commit() ->try의 가장 마지막 부분 rollback() ->catch
	 * 
	 * @param senderAccountNo
	 * @param password
	 * @param money
	 * @param receiverAccountNo
	 * @throws NoMoneyException
	 * @throws AccountNotFoundException
	 * @throws SQLException
	 * @throws NotMatchedPasswordException
	 * @throws InsufficientBalanceException
	 */
	public void transfer(String senderAccountNo, String password, int money, String receiverAccountNo)
			throws NoMoneyException, SQLException, AccountNotFoundException, NotMatchedPasswordException,
			InsufficientBalanceException {
		if (money <= 0)
			throw new NoMoneyException("이채액은 0원을 초괴해야 합니다");
		if (existAccountNo(receiverAccountNo) == false)
			throw new AccountNotFoundException("이체받을 계좌가 존재하지 않습니다");
		int balance = findBalanceByAccountNo(senderAccountNo, password);
		if (balance < money) {
			throw new InsufficientBalanceException("잔액 부족으로 이체할 수 없습니다.");
		}
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = getConnection();
			//수동커밋모드로 설정-> 트랜잭션 제어를 위해 
			con.setAutoCommit(false);
			String withdrawSql ="UPDATE account SET balance=balance-? WHERE account_no=?";
			pstmt = con.prepareCall(withdrawSql);
			pstmt.setInt(1, money);
			pstmt.setString(2, senderAccountNo);
			pstmt.executeUpdate();
			pstmt.close();
			//이체 받는 사람의 계좌에 입금
			String depositSql ="UPDATE account SET balance=balance+? WHERE account_no=?";
			pstmt = con.prepareStatement(depositSql);
			pstmt.setInt(1, money);
			pstmt.setString(	2, receiverAccountNo);
			pstmt.executeUpdate();
			con.commit(); //출금, 입금 모든 작업이 정상적으로 처리되면 실제 db에 반영한다.
		}catch(Exception e) {
			con.rollback();
			throw e;
		} finally {
			closeAll(pstmt, con);
		}

	}

	public ArrayList<AccountVO> findHighestBalanceAccount() throws SQLException {
		ArrayList<AccountVO> list = new ArrayList<AccountVO>();
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			//String sql = "SELECT account_no,name,balance FROM account WHERE balance=(SELECT MAX(balance) FROM account)";
			StringBuilder sql = new StringBuilder("SELECT account_no,name,balance ");
			sql.append("FROM account ");
			sql.append("WHERE balance=(SELECT MAX(balance) FROM account)");
			//pstmt = con.prepareStatement(sql);
			pstmt = con.prepareStatement(sql.toString());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add(new AccountVO(rs.getString("account_no"),rs.getInt("balance"),rs.getString("name")));
				//list.add(new AccountVO( rs.getInt(1), rs.getString(2),rs.getString(3)));
		} finally {
			closeAll(rs, pstmt, con);
		}
		return list;
	}

}
