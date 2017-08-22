package com.pokevian.app.smartfleet.model;



/**
 * 관리자 페이지 > Drive Ranking > 에코점수
 * @author Shin HyunJae
 */
public class MemberEcoData extends EcoData {

    private float     safeEco;                                            // 에코점수
    private int     safe;                                               // 안전점수
    private String  loginId;                                            // 회원계정
    private String  memberNo;											// 회원번호
    private String  memberNm;											// 회원명
    private String  email;                                              // 이메일
    private float   fuelEconomy;                                        // 평균연비
    private String  carModelName;                                       // 차량모델
    private String  carSrcName;                                         // 유종
    private int engineDisplacement;                                     // 배기량(CC)
    private float officalFuelEconomy;                                   // 공인연비(KM/L)
    private float avgDrivingDistance;                                   // 1회 평균 운행거리(KM)
    private int	rank;													// 등수
    private String localCd;												// 지역 코드 
    private String localName;											// 지역 이름
    
    public MemberEcoData() {
    }

    public float getSafeEco() {
        return safeEco;
    }

    public void setSafeEco(float safeEco) {
        this.safeEco = safeEco;
    }

    public int getSafe() {
        return safe;
    }

    public void setSafe(int safe) {
        this.safe = safe;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getMemberNm() {
        return memberNm;
    }

    public void setMemberNm(String memberNm) {
        this.memberNm = memberNm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public float getFuelEconomy() {
        return fuelEconomy;
    }

    public void setFuelEconomy(float fuelEconomy) {
        this.fuelEconomy = fuelEconomy;
    }

    public String getCarModelName() {
        return carModelName;
    }

    public void setCarModelNmCd(String carModelName) {
        this.carModelName = carModelName;
    }

    public String getCarSrcName() {
        return carSrcName;
    }

    public void setCarSrcName(String carSrcName) {
        this.carSrcName = carSrcName;
    }

    public int getEngineDisplacement() {
        return engineDisplacement;
    }

    public void setEngineDisplacement(int engineDisplacement) {
        this.engineDisplacement = engineDisplacement;
    }

    public float getOfficalFuelEconomy() {
        return officalFuelEconomy;
    }

    public void setOfficalFuelEconomy(float officalFuelEconomy) {
        this.officalFuelEconomy = officalFuelEconomy;
    }

    public float getAvgDrivingDistance() {
        return avgDrivingDistance;
    }

    public void setAvgDrivingDistance(float avgDrivingDistance) {
        this.avgDrivingDistance = avgDrivingDistance;
    }
    
    public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	
	public String getLocalCd() {
		return localCd;
	}

	public void setLocalCd(String localCd) {
		this.localCd = localCd;
	}

	public String getLocalName() {
		return localName;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	@Override
	public String toString() {
		return "MemberEcoData [" +
				"super" + super.toString() + 
				"safeEco=" + safeEco 
				+ ", loginId=" + loginId
				+ ", email=" + email
				+ ", memberNm=" + memberNm
				+ ", fuelEconomy=" + fuelEconomy
				+ ", carModelName=" + carModelName
				+ ", carSrcName=" + carSrcName
				+ ", engineDisplacement=" + engineDisplacement
				+ ", officalFuelEconomy=" + officalFuelEconomy
				+ ", avgDrivingDistance=" + avgDrivingDistance
				+ ", rank=" + rank
				+ ", localName=" + localName				
				+ "]";
	}
    
}