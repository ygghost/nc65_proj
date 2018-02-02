package nc.ui.so.m30.billui.action.printaction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import nc.ui.pubapp.uif2app.actions.OutputAction;
import nc.ui.pubapp.uif2app.model.BillManageModel;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.so.m30.entity.SaleOrderHVO;
import nc.vo.so.m30.entity.SaleOrderVO;
import nc.vo.so.pub.enumeration.BillStatus;
public class ExtOutputAction extends OutputAction{
	private static final long serialVersionUID = 527849621398524782L;
	
	@Override
	public void doAction(ActionEvent e) throws Exception {
		//---begin-----未审批通过的销售订单不能打印预览输出--modified--by--yegz--2017-10-25----------//
		//存放未审批通过的销售订单号
		List<String> vbillnos=new ArrayList<String>();
        Object[] objs=((BillManageModel) this.getModel()).getSelectedOperaDatas();
        if(null!=objs&&objs.length>0){
        	for (int i=0;i<objs.length;i++) {
        		SaleOrderVO aggVO=(SaleOrderVO) objs[i];
        		SaleOrderHVO hvo=aggVO.getParentVO();
        		//销售订单状态不等于审批通过或者不等于关闭，不能打印、预览与输出
        		if(BillStatus.I_AUDIT != hvo.getFstatusflag() && BillStatus.I_CLOSED !=hvo.getFstatusflag()){
        			vbillnos.add(hvo.getVbillcode());
        		}
			}
    		if (vbillnos.size()>0) {
    			String errmsg="";
    			for (String billno : vbillnos) {
    				String msg="销售订单【" +billno+ "】未审批通过或关闭状态不能输出！\n";
    				errmsg=errmsg+msg;
    			}
    			ExceptionUtils.wrappBusinessException(errmsg);
    		}
        }
       //---end-----未审批通过的销售订单不能打印预览输出--modified--by--yegz--2017-10-25----------//

		super.doAction(e);
	}

}
