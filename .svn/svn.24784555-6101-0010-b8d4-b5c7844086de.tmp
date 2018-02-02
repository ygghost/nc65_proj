/**
 *
 */
package nc.ui.so.m30.billui.action;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nc.bs.framework.common.NCLocator;
import nc.desktop.quickcode.MessageDlg;
import nc.itf.pubapp.pub.exception.IResumeException;
import nc.itf.so.m30.ISaleOrgPubService;
import nc.itf.uap.IUAPQueryBS;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.md.data.access.NCObject;
import nc.ui.ml.NCLangRes;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pubapp.uif2app.actions.RefreshSingleAction;
import nc.ui.pubapp.uif2app.actions.pflow.SaveScriptAction;
import nc.ui.scmpub.util.ResumeExceptionUIProcessUtils;
import nc.ui.so.m30.billui.view.SaleOrderBillForm;
import nc.ui.so.pub.keyvalue.CardKeyValue;
import nc.ui.uif2.UIState;
import nc.vo.bd.feature.ffile.entity.AggFFileVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.AppContext;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.pubapp.pattern.pub.MathTool;
import nc.vo.pubapp.pattern.pub.PubAppTool;
import nc.vo.pubapp.pflow.PfUserObject;
import nc.vo.so.m30.entity.OffsetTempVO;
import nc.vo.so.m30.entity.SaleOrderBVO;
import nc.vo.so.m30.entity.SaleOrderHVO;
import nc.vo.so.m30.entity.SaleOrderUserObject;
import nc.vo.so.m30.entity.SaleOrderVO;
import nc.vo.so.m30.sobalance.entity.SoBalanceVO;
import nc.vo.so.m30.util.FeatureSelectUtil;
import nc.vo.so.m30.util.SaleOrderClientContext;
import nc.vo.so.m30trantype.entity.M30TranTypeVO;
import nc.vo.so.pub.SOConstant;
import nc.vo.so.pub.enumeration.BillStatus;

/**
 * 销售订单保存逻辑
 * 
 * @since 6.0
 * @version 2011-12-28 上午10:26:42
 * @author fengjb
 */
public class SaleOrderSaveAction extends SaveScriptAction {

	private static final long serialVersionUID = -3977967248003982108L;

	private RefreshSingleAction refreshAction;

	/**
	 * 构造方法
	 */
	public SaleOrderSaveAction() {
		super();
	}

	@Override
	public void doAction(ActionEvent e) throws Exception {
		SaleOrderVO saleorder = (SaleOrderVO) this.editor.getValue();
		SaleOrderBillForm billform = (SaleOrderBillForm) this.editor;
		CardKeyValue keyValue = new CardKeyValue(billform.getBillCardPanel());
		if (this.getModel().getUiState() == UIState.EDIT) {
			int index = this.getModel().findBusinessData(saleorder);
			if (index == -1) {
				ExceptionUtils
						.wrappBusinessException(nc.vo.ml.NCLangRes4VoTransl
								.getNCLangRes().getStrByID("4006011_0",
										"04006011-0019")/*
														 * @res
														 * "修改保存时，获取前台差异VO出错。"
														 */);
			}
			// 订单收款限额不能小于实际预收款金额
			this.checkGathering(saleorder);
			// 审批不同过状态重置为自由态
			this.reSetBillStatusForNoPass(saleorder);
		}
		// 前台增加表头ID，防止网络出问题时重复保存单据的情况
		if (this.getModel().getUiState() == UIState.ADD) {
			String hID = keyValue.getHeadStringValue(SaleOrderHVO.CSALEORDERID);
			if (PubAppTool.isNull(hID)) {
				ISaleOrgPubService service = NCLocator.getInstance().lookup(
						ISaleOrgPubService.class);
				String[] ids = service.getOIDArray(1);
				keyValue.setHeadValue(SaleOrderHVO.CSALEORDERID, ids[0]);
			}
		}
		// 赠品兑付类型不能为空
		this.checkCarsubtypeid(saleorder, keyValue);
		// --begin-------销售订单保存增加物料可用量检查----Add by WYR 2017-11-16----------//
		if(checkMaterialKYL(saleorder)){//返回true不能保存和保存提交
			return;
		}
		// --end-------销售订单保存增加物料可用量检查----Add by WYR 2017-11-16------------//
		super.doAction(e);
		this.doAfterAction();
	}

	/**
	 * 保存后事件处理
	 */
	private void doAfterAction() {
		// 界面用mix("本次收款金额"||"价税合计")更新"实际收款金额",并清空"本次收款金额"
		SaleOrderBillForm billform = (SaleOrderBillForm) this.editor;
		CardKeyValue keyValue = new CardKeyValue(billform.getBillCardPanel());
		UFDouble thisreceivemny = keyValue
				.getHeadUFDoubleValue(SaleOrderHVO.NTHISRECEIVEMNY);
		UFDouble receivedmny = keyValue
				.getHeadUFDoubleValue(SaleOrderHVO.NRECEIVEDMNY);
		UFDouble totalorigmny = keyValue
				.getHeadUFDoubleValue(SaleOrderHVO.NTOTALORIGMNY);

		UFDouble receivedmny_new = MathTool.add(thisreceivemny, receivedmny);
		// 本次收款金额
		if (MathTool.absCompareTo(receivedmny_new, totalorigmny) > 0) {
			receivedmny_new = totalorigmny;
		}

		keyValue.setHeadValue(SaleOrderHVO.NRECEIVEDMNY, receivedmny_new);
		keyValue.setHeadValue(SaleOrderHVO.NTHISRECEIVEMNY, null);
		FeatureSelectUtil.clearAllRowValue(keyValue, SOConstant.AGGFFILEVO);
	}

	@Override
	public void doBeforAction() {
		super.doBeforAction();
		SaleOrderBillForm billform = (SaleOrderBillForm) this.editor;
		if (null != billform) {
			CardKeyValue keyValue = new CardKeyValue(
					billform.getBillCardPanel());
			PfUserObject pfUserObj = this.getFlowContext().getUserObj();
			pfUserObj = pfUserObj == null ? new PfUserObject() : pfUserObj;
			SaleOrderUserObject userobj = (SaleOrderUserObject) (pfUserObj
					.getUserObject() == null ? new SaleOrderUserObject()
					: pfUserObj.getUserObject());
			// 费用冲抵
			OffsetTempVO tempvo = billform.getTempvo();
			userobj.setOffsetTempVO(tempvo);
			// 订单核销
			SoBalanceVO sobalancevo = billform.getSobalancevo();
			userobj.setSoBalanceVO(sobalancevo);
			// 本次收款金额
			UFDouble thisGatheringMny = billform.getThisGatheringMny();
			userobj.setThisGatheringMny(thisGatheringMny);
			Map<String, AggFFileVO> aggffilevomap = FeatureSelectUtil
					.getAllRowAggFFileVO(keyValue);
			if (aggffilevomap.size() > 0) {
				userobj.setAggffilevomap(aggffilevomap);
			}
			userobj.setIsclientsave(true);
			pfUserObj.setUserObject(userobj);
			this.getFlowContext().setUserObj(pfUserObj);
		}
	}

	private void checkGathering(SaleOrderVO saleorder) {
		SaleOrderHVO hvo = saleorder.getParentVO();
		if (null == hvo) {
			return;
		}
		// 收款限额控制预收
		UFBoolean bpreceiveflag = hvo.getBpreceiveflag();
		// 订单收款限额
		UFDouble npreceivequota = hvo.getNpreceivequota();
		// 实际预收款金额
		UFDouble npreceivemny = hvo.getNpreceivemny();

		if (bpreceiveflag.booleanValue()
				&& MathTool.compareTo(npreceivequota, npreceivemny) < 0) {
			StringBuilder errMsg = new StringBuilder();
			errMsg.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
					.getStrByID("4006011_0", "04006011-0283")/* 单据号： */);
			errMsg.append(hvo.getVbillcode());
			errMsg.append(nc.vo.ml.NCLangRes4VoTransl.getNCLangRes()
					.getStrByID("4006011_0", "04006011-0468")/*
															 * 订单收款限额小于实际预收款金额,
															 * 请重新调整价税合计！
															 */);
			ExceptionUtils.wrappBusinessException(errMsg.toString());
		}
	}

	private void checkCarsubtypeid(SaleOrderVO saleorder, CardKeyValue keyValue) {
		SaleOrderHVO hvo = saleorder.getParentVO();
		if (null == hvo) {
			return;
		}
		SaleOrderBillForm billform = (SaleOrderBillForm) this.editor;
		String tranTypeCode = keyValue
				.getHeadStringValue(SaleOrderHVO.VTRANTYPECODE);
		String pk_group = AppContext.getInstance().getPkGroup();
		SaleOrderClientContext cache = billform.getM30ClientContext();
		M30TranTypeVO m30transvo = cache.getTransType(tranTypeCode, pk_group);
		if (m30transvo != null) {
			if (m30transvo.getBlrgcashflag().booleanValue()) {
				// 赠品兑付类型
				String carsubtypeid = hvo.getCarsubtypeid();
				if (PubAppTool.isNull(carsubtypeid)) {
					ExceptionUtils.wrappBusinessException(NCLangRes
							.getInstance().getStrByID("4006011_0",
									"04006011-0506")/* 赠品兑付类型不允许为空。 */);
				}
			}
		}
	}

	/**
	 * 
	 * @return RefreshSingleAction
	 */
	public RefreshSingleAction getreFreshAction() {
		return this.refreshAction;
	}

	/**
	 * 
	 * @param refreshAction1
	 */
	public void setRefreshAction(RefreshSingleAction refreshAction1) {
		this.refreshAction = refreshAction1;
	}

	@Override
	protected boolean isResume(IResumeException resumeInfo) {
		return ResumeExceptionUIProcessUtils.isResume(resumeInfo,
				getFlowContext());
	}

	@Override
	protected Object[] processBefore(Object[] vos) {
		for (Object vo : vos) {
			SaleOrderVO saleordervo = (SaleOrderVO) vo;
			SaleOrderBVO[] bvos = saleordervo.getChildrenVO();
			if (bvos == null || bvos.length == 0) {

				ExceptionUtils
						.wrappBusinessException(nc.vo.ml.NCLangRes4VoTransl
								.getNCLangRes().getStrByID("4006011_0",
										"04006011-0020")/* @res "表体数据为空，不允许保存。" */);
			}
		}
		return vos;
	}

	private void reSetBillStatusForNoPass(SaleOrderVO vo) {
		if (vo.getParentVO().getFstatusflag().intValue() == BillStatus.NOPASS
				.getIntValue()) {
			vo.getParentVO().setFstatusflag(BillStatus.FREE.getIntegerValue());
			this.editor.setValue(vo);
		}
	}
	
	
	/**
	 * 校验物料可用量
	 * 
	 * @param saleorder 销售订单聚合VO
	 * @author WYR 
	 * @Date 2017-11-16
	 * 
	 */
	private boolean checkMaterialKYL(SaleOrderVO saleorder)throws BusinessException{
		List<String> msglist=new ArrayList<String>();
		boolean flag=false;
		if(null==saleorder)return flag;
		SaleOrderHVO hvo=saleorder.getParentVO();
		SaleOrderBVO[] bvos=saleorder.getChildrenVO();
		if (bvos == null || bvos.length == 0)return flag;
		String salesorgCode=NCObject.newInstance(hvo).getAttributeValue("pk_org.code").toString();
		String salesorgName=NCObject.newInstance(hvo).getAttributeValue("pk_org.name").toString();
		Map<String,UFDouble> materialMap=new HashMap<String,UFDouble>();
		Map<String,SaleOrderBVO> bvoMap=new HashMap<String,SaleOrderBVO>();
		int i=0;
		for (SaleOrderBVO bvo : bvos) {
			if(null!=bvo.getCmaterialvid()&&(null!=bvo.getNnum()&&bvo.getNnum().toDouble()!=0)&&null!=bvo.getCsendstordocid()){
				String key=bvo.getCmaterialvid()+bvo.getCsendstordocid();
				bvoMap.put(key,bvo);
				if(i==0){
					materialMap.put(key, bvo.getNnum());
				}
				if(i>0){
					if(null==materialMap.get(key)){
						materialMap.put(key, bvo.getNnum());
					}else{
						UFDouble dnum=materialMap.get(key);
						dnum=dnum.add(bvo.getNnum());
						materialMap.put(key, dnum);
					}
				}
			}
			i++;
		}	
		if(materialMap.size()>0){
			for (Map.Entry<String, UFDouble> entry : materialMap.entrySet()) { 
				  String key=entry.getKey();
				  UFDouble nnum =entry.getValue(); 
				  SaleOrderBVO salebvo=bvoMap.get(key);
				  String materialCode=NCObject.newInstance(salebvo).getAttributeValue("cmaterialvid.code").toString();
				  String materialName=NCObject.newInstance(salebvo).getAttributeValue("cmaterialvid.name").toString();
				  String stordocCode=NCObject.newInstance(salebvo).getAttributeValue("csendstordocid.code").toString();
				  String stordocName=NCObject.newInstance(salebvo).getAttributeValue("csendstordocid.name").toString();
				  String measdocName=NCObject.newInstance(salebvo).getAttributeValue("cunitid.name").toString();
				  //begin--added by yegz on 2018-01-11
				  String sendstockorgcode=NCObject.newInstance(salebvo).getAttributeValue("csendstockorgid.code").toString();
				  //end--added by yegz on 2018-01-11
				  //begin--modified by yegz on 2018-01-11
				  //UFDouble  kyl=queryMaertailKYL(salesorgCode,stordocCode,materialCode);
				  UFDouble  kyl=queryMaertailKYL(sendstockorgcode,stordocCode,materialCode);
				  //end--modified by yegz on 2018-01-11
				  String strkyl=getFormatNumber(kyl.toString());
				  if(kyl.toDouble()==0){
					  String msg="物料【"+materialCode+"】"+"可用量不足或未找到该物料可用量!\n";
					  msglist.add(msg);
				  }
				  if(kyl.toDouble()!=0&&kyl.toDouble()<nnum.toDouble()){
					  String msg="物料【"+materialCode+"】"+"可用量不足，现可用量为"+strkyl+measdocName+"!\n";
					  msglist.add(msg);
				  }
				}
		}
		if(msglist.size()>0){
			String errmsg="";
			for (String msg : msglist) {
				errmsg=errmsg+msg;
			}
			errmsg=errmsg+"是否继续保存或保存提交？";
		   int n=MessageDialog.showYesNoCancelDlg( (SaleOrderBillForm) this.editor, "提示", errmsg);
		   if(UIDialog.ID_YES!=n){
			   flag=true;
		   }
			
		}
		return flag;
	}
	
	/***
	 * 查询物料可用量
	 * 
	 * @param salesorgCode 销售组织编码
	 * @param stordocCode 发货仓库编码
	 * @param materialCode 物料编码
	 * @author WYR 
	 * @Date 2017-11-16
	 * @return
	 * @throws BusinessException 
	 */
	private UFDouble queryMaertailKYL(String salesorgCode,String stordocCode,String materialCode) throws BusinessException {
		UFDouble kyl=new UFDouble(0);
		String sql="select kyl from v_sscl where orgcode='"+salesorgCode+"' and storcode='"+stordocCode+"' and  code='"+materialCode+"'";
		IUAPQueryBS queryservice=(IUAPQueryBS) NCLocator.getInstance().lookup(IUAPQueryBS.class.getName());
		Object value=queryservice.executeQuery(sql, new ColumnProcessor());
	    if(null!=value){
	    	kyl=new UFDouble(value.toString());
	    }
		return kyl;
	}
	
	/**
	 * 格式数值字段(保留2位小数)
	 *
	 * @param str 数值字符串
	 * @author WYR 
	 * @Date 2017-11-16
	 * @return
	 */
	public static String getFormatNumber(String str) {
	    if (str == null||"".equals(str)) str = "0";
	    BigDecimal bd = new BigDecimal(str);
	    double fAmount = bd.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
	    String sFormat = String.format("%1$.3f", fAmount);
	    return sFormat;
//	    if (sFormat == null) sFormat = "0.000";
//	    String formatAmount = sFormat.substring(0, sFormat.lastIndexOf(".") + 4);
//	    return formatAmount;
	}

}
