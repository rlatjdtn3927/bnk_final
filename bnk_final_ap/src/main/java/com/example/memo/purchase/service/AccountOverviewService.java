// AP - com/example/memo/purchase/service/AccountOverviewService.java
package com.example.memo.purchase.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountOverviewService {

    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;
    private final ObjectMapper om;

    public ObjectNode getOverview(JsonNode data) {
        long userId = data.path("userId").asLong(0L);                       // IRP용
        Long dcMemberId = data.hasNonNull("dcMemberId") ?                   // DC용
                data.get("dcMemberId").asLong() : null;

        ObjectNode root = om.createObjectNode();

        // IRP
        Optional<IrpAccount> irpOpt = (userId == 0L) ? Optional.empty()
                : irpRepo.findTopByUserUserIdAndStatusNot(userId, "CLOSED");

        ObjectNode irpNode = om.createObjectNode();
        if (irpOpt.isPresent()) {
            IrpAccount a = irpOpt.get();
            ObjectNode acct = om.createObjectNode();
            acct.put("irpAcctNo", a.getIrpAcctNo());
            acct.put("status", a.getStatus() == null ? "NORMAL" : a.getStatus());
            irpNode.put("exists", true);
            irpNode.set("account", acct);
        } else {
            irpNode.put("exists", false);
        }
        root.set("irp", irpNode);

        // DC (dcMemberId가 있어야 조회)
        ObjectNode dcNode = om.createObjectNode();
        if (dcMemberId != null) {
            List<DcAccount> dcList =
                /* 엔티티에 맞는 쪽 하나 선택 */
                // dcRepo.findByDcMember_IdAndStatusNot(dcMemberId, "CLOSED");
                dcRepo.findByDcMember_IdAndStatusNot(dcMemberId, "CLOSED");

            if (dcList != null && !dcList.isEmpty()) {
                ArrayNode arr = om.createArrayNode();
                for (DcAccount dc : dcList) {
                    ObjectNode o = om.createObjectNode();
                    o.put("accountNo", dc.getAccountNo());
                    o.put("status", dc.getStatus() == null ? "NORMAL" : dc.getStatus());
                    arr.add(o);
                }
                dcNode.put("exists", true);
                dcNode.set("accounts", arr);
            } else {
                dcNode.put("exists", false);
                dcNode.set("accounts", om.createArrayNode());
                root.put("message", "해당 가입자명부에 DC 계좌가 없습니다.");
            }
        } else {
            dcNode.put("exists", false);
            dcNode.set("accounts", om.createArrayNode());
            root.put("message", "DC 가입자명부 식별값(dcMemberId)이 없습니다.");
        }
        root.set("dc", dcNode);

        return root;
    }
}
