package test2_study2;

import java.sql.SQLException;

import model.MemberDAO;
import model.MemberVO;

/*
 * 회원정보를 member TABLE에 INSERT하는 예제
 */
public class TestMemberDAO1 {
	public static void main(String[] args) {
		try {
			MemberDAO dao = new MemberDAO();
			// INSERT 즉, 등록할 회원 정보
			MemberVO vo = new MemberVO("javaking", "b", "권용은", "서울");
			dao.registerMember(vo);
			System.out.println("등록완료");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
